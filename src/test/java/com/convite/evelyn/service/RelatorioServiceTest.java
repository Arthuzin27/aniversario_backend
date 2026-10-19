package com.convite.evelyn.service;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.dto.ConvidadoComFamilia;
import com.convite.evelyn.dto.RelatorioConvidadosResponse;
import com.convite.evelyn.repository.ConvidadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    ConvidadoRepository convidadoRepository;

    @Test
    void ordenaIgnorandoMaiusculasComAcentosEmPosicaoCorretaENumera() {
        when(convidadoRepository.findTodosComFamilia()).thenReturn(List.of(
                new ConvidadoComFamilia(1L, "pedro Silva", "Família Silva"),
                new ConvidadoComFamilia(2L, "Ana Souza", "Família Souza"),
                new ConvidadoComFamilia(3L, "Érica Lima", "Família Lima"),
                new ConvidadoComFamilia(4L, "carlos Silva", "Família Silva"),
                new ConvidadoComFamilia(5L, "Bruno Oliveira", "Família Oliveira"),
                new ConvidadoComFamilia(6L, "ana souza", "Família Abreu")));

        RelatorioConvidadosResponse relatorio = new RelatorioService(convidadoRepository, properties()).gerar();

        assertThat(relatorio.convidados()).extracting(RelatorioConvidadosResponse.Item::nomeCompleto)
                .containsExactly("ana souza", "Ana Souza", "Bruno Oliveira", "carlos Silva", "Érica Lima", "pedro Silva");
        assertThat(relatorio.convidados()).extracting(RelatorioConvidadosResponse.Item::numero)
                .containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(relatorio.totalPessoas()).isEqualTo(6);
        assertThat(relatorio.titulo()).isEqualTo("LISTA GERAL DE CONVIDADOS");
        assertThat(relatorio.geradoEm().getOffset()).isEqualTo(ZoneId.of("America/Sao_Paulo").getRules()
                .getOffset(relatorio.geradoEm().toInstant()));
    }

    private static AppProperties properties() {
        return new AppProperties(
                new AppProperties.Convite(30),
                new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Jwt("x".repeat(32), Duration.ofHours(1)),
                new AppProperties.Admin(null, null, false),
                new AppProperties.RateLimit(10, 5),
                new AppProperties.Relatorio(ZoneId.of("America/Sao_Paulo")));
    }
}
