package com.convite.evelyn.service;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.dto.CadastroConfirmadoResponse;
import com.convite.evelyn.dto.EstatisticasResponse;
import com.convite.evelyn.dto.FamiliaAtualizacaoRequest;
import com.convite.evelyn.dto.FamiliaAtualizacaoRequest.ConvidadoItem;
import com.convite.evelyn.dto.FamiliaCadastroRequest;
import com.convite.evelyn.dto.FamiliaResponse;
import com.convite.evelyn.dto.FamiliaResponse.ConvidadoResponse;
import com.convite.evelyn.dto.PaginaResponse;
import com.convite.evelyn.entity.Convidado;
import com.convite.evelyn.entity.Familia;
import com.convite.evelyn.exception.RecursoNaoEncontradoException;
import com.convite.evelyn.exception.RegraNegocioException;
import com.convite.evelyn.repository.ConvidadoRepository;
import com.convite.evelyn.repository.FamiliaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FamiliaService {

    private static final Logger log = LoggerFactory.getLogger(FamiliaService.class);

    private final FamiliaRepository familiaRepository;
    private final ConvidadoRepository convidadoRepository;
    private final AppProperties properties;

    public FamiliaService(FamiliaRepository familiaRepository, ConvidadoRepository convidadoRepository,
                          AppProperties properties) {
        this.familiaRepository = familiaRepository;
        this.convidadoRepository = convidadoRepository;
        this.properties = properties;
    }

    /** Resultado do cadastro público; {@code criado=false} indica repetição de uma requisição já processada. */
    public record ResultadoCadastro(CadastroConfirmadoResponse resposta, boolean criado) {
    }

    /**
     * Cadastra a família e todos os participantes em uma única transação.
     * Se a mesma chave de idempotência já foi usada, devolve o cadastro existente sem duplicar.
     */
    @Transactional
    public ResultadoCadastro cadastrar(FamiliaCadastroRequest request, UUID chaveIdempotencia) {
        if (chaveIdempotencia != null) {
            Optional<Familia> existente = familiaRepository.findByChaveIdempotencia(chaveIdempotencia);
            if (existente.isPresent()) {
                log.info("Requisição repetida ignorada (idempotência): familiaId={}", existente.get().getId());
                return new ResultadoCadastro(paraConfirmacao(existente.get()), false);
            }
        }

        String nome = validarNomeFamilia(request.nome());
        List<String> nomes = request.convidados().stream().map(NomeUtils::normalizar).toList();
        validarParticipantes(nomes);
        if (request.quantidadePessoas() != nomes.size()) {
            throw new RegraNegocioException("convidados",
                    "A quantidade de pessoas (" + request.quantidadePessoas()
                            + ") não corresponde à quantidade de nomes informados (" + nomes.size() + ").");
        }

        Familia familia = new Familia(nome, chaveIdempotencia);
        nomes.forEach(familia::adicionarConvidado);
        familia = familiaRepository.saveAndFlush(familia);

        log.info("Família cadastrada: familiaId={}, pessoas={}", familia.getId(), nomes.size());
        return new ResultadoCadastro(paraConfirmacao(familia), true);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<FamiliaResponse> listar(String busca, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "dataCadastro").and(Sort.by(Sort.Direction.DESC, "id")));
        String termo = NomeUtils.normalizar(busca);
        Page<Familia> page = (termo == null || termo.isEmpty())
                ? familiaRepository.findAll(pageRequest)
                : familiaRepository.buscar("%" + escaparLike(termo.toLowerCase(Locale.ROOT)) + "%", pageRequest);

        List<Long> ids = page.getContent().stream().map(Familia::getId).toList();
        Map<Long, List<ConvidadoResponse>> convidadosPorFamilia = ids.isEmpty() ? Map.of()
                : convidadoRepository.findByFamiliaIds(ids).stream().collect(Collectors.groupingBy(
                        c -> c.getFamilia().getId(),
                        Collectors.mapping(c -> new ConvidadoResponse(c.getId(), c.getNomeCompleto()), Collectors.toList())));
        Set<String> repetidos = nomesRepetidos(page.getContent());

        List<FamiliaResponse> conteudo = page.getContent().stream()
                .map(f -> {
                    List<ConvidadoResponse> convidados = convidadosPorFamilia.getOrDefault(f.getId(), List.of());
                    return new FamiliaResponse(f.getId(), f.getNome(), convidados.size(), convidados,
                            f.getDataCadastro(), f.getDataAtualizacao(), repetidos.contains(chaveNome(f.getNome())));
                })
                .toList();
        return PaginaResponse.de(page, conteudo);
    }

    @Transactional(readOnly = true)
    public FamiliaResponse buscarPorId(Long id) {
        Familia familia = carregar(id);
        return paraResposta(familia, !nomesRepetidos(List.of(familia)).isEmpty());
    }

    /**
     * Atualiza o nome e sincroniza a lista de participantes: atualiza os existentes,
     * cria os novos (sem id) e remove os que não foram enviados.
     */
    @Transactional
    public FamiliaResponse atualizar(Long id, FamiliaAtualizacaoRequest request) {
        Familia familia = carregar(id);
        String nome = validarNomeFamilia(request.nome());
        List<String> nomes = request.convidados().stream().map(c -> NomeUtils.normalizar(c.nomeCompleto())).toList();
        validarParticipantes(nomes);

        Map<Long, Convidado> atuais = new HashMap<>();
        familia.getConvidados().forEach(c -> atuais.put(c.getId(), c));
        Set<Long> mantidos = new HashSet<>();
        for (ConvidadoItem item : request.convidados()) {
            if (item.id() != null && (!atuais.containsKey(item.id()) || !mantidos.add(item.id()))) {
                throw new RegraNegocioException("convidados",
                        "O participante de ID " + item.id() + " não pertence a esta família ou foi informado mais de uma vez.");
            }
        }

        familia.setNome(nome);
        atuais.values().stream().filter(c -> !mantidos.contains(c.getId())).toList()
                .forEach(familia::removerConvidado);
        for (ConvidadoItem item : request.convidados()) {
            String nomeCompleto = NomeUtils.normalizar(item.nomeCompleto());
            if (item.id() == null) {
                familia.adicionarConvidado(nomeCompleto);
            } else {
                atuais.get(item.id()).setNomeCompleto(nomeCompleto);
            }
        }
        familia.marcarAtualizacao();
        familiaRepository.saveAndFlush(familia);

        log.info("Família atualizada: familiaId={}, pessoas={}", familia.getId(), familia.getConvidados().size());
        return paraResposta(familia, !nomesRepetidos(List.of(familia)).isEmpty());
    }

    @Transactional
    public void excluir(Long id) {
        Familia familia = carregar(id);
        familiaRepository.delete(familia);
        log.info("Família excluída: familiaId={}", id);
    }

    @Transactional(readOnly = true)
    public EstatisticasResponse estatisticas() {
        long totalFamilias = familiaRepository.count();
        long totalPessoas = convidadoRepository.count();
        BigDecimal media = totalFamilias == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalPessoas).divide(BigDecimal.valueOf(totalFamilias), 2, RoundingMode.HALF_UP);
        return new EstatisticasResponse(totalPessoas, totalFamilias, media);
    }

    private Familia carregar(Long id) {
        return familiaRepository.findComConvidadosById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Família não encontrada."));
    }

    private String validarNomeFamilia(String nomeInformado) {
        String nome = NomeUtils.normalizar(nomeInformado);
        if (nome == null || nome.isEmpty()) {
            throw new RegraNegocioException("nome", "Informe o nome da família.");
        }
        return nome;
    }

    private void validarParticipantes(List<String> nomes) {
        int max = properties.convite().maxParticipantes();
        if (nomes.isEmpty()) {
            throw new RegraNegocioException("convidados", "Informe pelo menos um participante.");
        }
        if (nomes.size() > max) {
            throw new RegraNegocioException("convidados",
                    "O limite é de " + max + " participantes por família.");
        }
        if (nomes.stream().anyMatch(n -> n == null || n.isEmpty())) {
            throw new RegraNegocioException("convidados", "Preencha o nome completo de todos os participantes.");
        }
    }

    private Set<String> nomesRepetidos(List<Familia> familias) {
        if (familias.isEmpty()) {
            return Set.of();
        }
        Set<String> nomes = familias.stream().map(f -> chaveNome(f.getNome())).collect(Collectors.toSet());
        return new HashSet<>(familiaRepository.nomesRepetidos(nomes));
    }

    private static String chaveNome(String nome) {
        return nome.toLowerCase(Locale.ROOT);
    }

    private static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private CadastroConfirmadoResponse paraConfirmacao(Familia familia) {
        List<String> nomes = familia.getConvidados().stream().map(Convidado::getNomeCompleto).toList();
        return new CadastroConfirmadoResponse(familia.getId(), familia.getNome(), nomes.size(), nomes,
                "Presença confirmada com sucesso! Obrigado, " + familia.getNome() + ".");
    }

    private FamiliaResponse paraResposta(Familia familia, boolean possivelDuplicata) {
        List<ConvidadoResponse> convidados = familia.getConvidados().stream()
                .map(c -> new ConvidadoResponse(c.getId(), c.getNomeCompleto()))
                .toList();
        return new FamiliaResponse(familia.getId(), familia.getNome(), convidados.size(), convidados,
                familia.getDataCadastro(), familia.getDataAtualizacao(), possivelDuplicata);
    }
}
