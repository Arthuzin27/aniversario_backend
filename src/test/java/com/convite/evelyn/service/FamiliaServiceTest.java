package com.convite.evelyn.service;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.dto.EstatisticasResponse;
import com.convite.evelyn.dto.FamiliaCadastroRequest;
import com.convite.evelyn.entity.Convidado;
import com.convite.evelyn.entity.Familia;
import com.convite.evelyn.exception.RecursoNaoEncontradoException;
import com.convite.evelyn.exception.RegraNegocioException;
import com.convite.evelyn.repository.ConvidadoRepository;
import com.convite.evelyn.repository.FamiliaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamiliaServiceTest {

    @Mock
    FamiliaRepository familiaRepository;

    @Mock
    ConvidadoRepository convidadoRepository;

    FamiliaService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Convite(3),
                new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Jwt("x".repeat(32), Duration.ofHours(1)),
                new AppProperties.Admin(null, null, false),
                new AppProperties.RateLimit(10, 5),
                new AppProperties.Relatorio(ZoneId.of("America/Sao_Paulo")));
        service = new FamiliaService(familiaRepository, convidadoRepository, properties);
    }

    @Test
    void cadastraFamiliaComNomesNormalizados() {
        when(familiaRepository.saveAndFlush(any(Familia.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.cadastrar(
                new FamiliaCadastroRequest("  Família   Silva ", 2, List.of(" Ana  Silva ", "Carlos Silva")), null);

        ArgumentCaptor<Familia> captor = ArgumentCaptor.forClass(Familia.class);
        verify(familiaRepository).saveAndFlush(captor.capture());
        Familia salva = captor.getValue();
        assertThat(salva.getNome()).isEqualTo("Família Silva");
        assertThat(salva.getConvidados()).extracting(Convidado::getNomeCompleto).containsExactly("Ana Silva", "Carlos Silva");
        assertThat(salva.getConvidados()).allSatisfy(c -> assertThat(c.getFamilia()).isSameAs(salva));
        assertThat(resultado.criado()).isTrue();
        assertThat(resultado.resposta().quantidadePessoas()).isEqualTo(2);
    }

    @Test
    void rejeitaQuantidadeDiferenteDaListaDeNomes() {
        assertThatThrownBy(() -> service.cadastrar(new FamiliaCadastroRequest("Família X", 3, List.of("A", "B")), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não corresponde");
        verify(familiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejeitaAcimaDoLimiteConfigurado() {
        assertThatThrownBy(() -> service.cadastrar(
                new FamiliaCadastroRequest("Família X", 4, List.of("A", "B", "C", "D")), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O limite é de 3 participantes por família.");
    }

    @Test
    void rejeitaNomeQueFicaVazioAposNormalizacao() {
        assertThatThrownBy(() -> service.cadastrar(new FamiliaCadastroRequest("Família X", 1, List.of("\t ")), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Preencha o nome completo de todos os participantes.");
        assertThatThrownBy(() -> service.cadastrar(new FamiliaCadastroRequest(" \n ", 1, List.of("Ana")), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Informe o nome da família.");
    }

    @Test
    void repeticaoComMesmaChaveDevolveCadastroExistenteSemSalvar() {
        UUID chave = UUID.randomUUID();
        Familia existente = new Familia("Família Lima", chave);
        existente.adicionarConvidado("Rita Lima");
        when(familiaRepository.findByChaveIdempotencia(chave)).thenReturn(Optional.of(existente));

        var resultado = service.cadastrar(new FamiliaCadastroRequest("Família Lima", 1, List.of("Rita Lima")), chave);

        assertThat(resultado.criado()).isFalse();
        assertThat(resultado.resposta().convidados()).containsExactly("Rita Lima");
        verify(familiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void calculaEstatisticas() {
        when(familiaRepository.count()).thenReturn(3L);
        when(convidadoRepository.count()).thenReturn(7L);

        EstatisticasResponse estatisticas = service.estatisticas();

        assertThat(estatisticas.totalPessoas()).isEqualTo(7);
        assertThat(estatisticas.totalFamilias()).isEqualTo(3);
        assertThat(estatisticas.mediaPessoasPorFamilia()).isEqualByComparingTo(new BigDecimal("2.33"));
    }

    @Test
    void estatisticasSemCadastrosNaoDividePorZero() {
        assertThat(service.estatisticas().mediaPessoasPorFamilia()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void excluirFamiliaInexistenteLancaNaoEncontrado() {
        when(familiaRepository.findComConvidadosById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
