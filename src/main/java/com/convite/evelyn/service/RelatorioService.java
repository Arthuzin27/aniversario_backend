package com.convite.evelyn.service;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.dto.ConvidadoComFamilia;
import com.convite.evelyn.dto.RelatorioConvidadosResponse;
import com.convite.evelyn.dto.RelatorioConvidadosResponse.Item;
import com.convite.evelyn.repository.ConvidadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Monta o relatório geral de convidados. A ordenação e a numeração são feitas aqui,
 * para que JSON, PDF e CSV tenham sempre exatamente o mesmo conteúdo.
 */
@Service
public class RelatorioService {

    public static final String TITULO = "LISTA GERAL DE CONVIDADOS";

    private final ConvidadoRepository convidadoRepository;
    private final AppProperties properties;

    public RelatorioService(ConvidadoRepository convidadoRepository, AppProperties properties) {
        this.convidadoRepository = convidadoRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public RelatorioConvidadosResponse gerar() {
        List<ConvidadoComFamilia> convidados = new ArrayList<>(convidadoRepository.findTodosComFamilia());
        convidados.sort(comparadorAlfabetico());

        List<Item> itens = new ArrayList<>(convidados.size());
        for (int i = 0; i < convidados.size(); i++) {
            ConvidadoComFamilia c = convidados.get(i);
            itens.add(new Item(i + 1, c.nomeCompleto(), c.familia()));
        }
        OffsetDateTime geradoEm = OffsetDateTime.now(properties.relatorio().fusoHorario()).truncatedTo(ChronoUnit.SECONDS);
        return new RelatorioConvidadosResponse(TITULO, geradoEm, itens, itens.size());
    }

    /**
     * Ordem alfabética em português, sem diferenciar maiúsculas de minúsculas
     * (acentos só desempatam: "Ana" e "ana" são equivalentes; "Álvaro" fica junto de "Alvaro").
     * Empates são resolvidos pelo nome da família e depois pelo ID, garantindo ordem estável.
     */
    static Comparator<ConvidadoComFamilia> comparadorAlfabetico() {
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.SECONDARY);
        return Comparator.comparing(ConvidadoComFamilia::nomeCompleto, collator)
                .thenComparing(ConvidadoComFamilia::familia, collator)
                .thenComparing(ConvidadoComFamilia::id);
    }
}
