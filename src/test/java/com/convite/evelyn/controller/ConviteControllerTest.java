package com.convite.evelyn.controller;

import com.convite.evelyn.IntegracaoTestBase;
import com.convite.evelyn.entity.Convidado;
import com.convite.evelyn.entity.Familia;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConviteControllerTest extends IntegracaoTestBase {

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void cadastraFamiliaValidaERetorna201() throws Exception {
        cadastrar("{\"nome\":\"Família Silva\",\"quantidadePessoas\":1,\"convidados\":[\"Carlos Silva\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Família Silva"))
                .andExpect(jsonPath("$.quantidadePessoas").value(1))
                .andExpect(jsonPath("$.mensagem").value(containsString("confirmada")));

        assertThat(familiaRepository.count()).isEqualTo(1);
        assertThat(convidadoRepository.count()).isEqualTo(1);
    }

    @Test
    void cadastraVariosConvidadosRemovendoEspacosExcedentesDeFormaConsistente() throws Exception {
        long id = cadastrarFamilia("  Família   Oliveira  ", "  Maria  Oliveira ", "Bruno Oliveira", "Pedro Oliveira");

        transactionTemplate.executeWithoutResult(tx -> {
            Familia familia = familiaRepository.findComConvidadosById(id).orElseThrow();
            assertThat(familia.getNome()).isEqualTo("Família Oliveira");
            assertThat(familia.getConvidados()).extracting(Convidado::getNomeCompleto)
                    .containsExactly("Maria Oliveira", "Bruno Oliveira", "Pedro Oliveira");
            assertThat(familia.getDataCadastro()).isNotNull();
            assertThat(familia.getConvidados()).allSatisfy(c -> assertThat(c.getFamilia().getId()).isEqualTo(id));
        });
        assertThat(convidadoRepository.count()).isEqualTo(3);
    }

    @Test
    void rejeitaNomeDeFamiliaVazio() throws Exception {
        cadastrar("{\"nome\":\"   \",\"quantidadePessoas\":1,\"convidados\":[\"Ana Souza\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].mensagem", hasItem("Informe o nome da família.")));
        assertThat(familiaRepository.count()).isZero();
    }

    @Test
    void rejeitaQuantidadeZeroNegativaOuAusente() throws Exception {
        for (String quantidade : List.of("0", "-2", "null")) {
            cadastrar("{\"nome\":\"Família Souza\",\"quantidadePessoas\":" + quantidade + ",\"convidados\":[\"Ana Souza\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[*].campo", hasItem("quantidadePessoas")));
        }
        assertThat(familiaRepository.count()).isZero();
    }

    @Test
    void rejeitaQuantidadeNaoInteira() throws Exception {
        cadastrar("{\"nome\":\"Família Souza\",\"quantidadePessoas\":1.5,\"convidados\":[\"Ana Souza\"]}")
                .andExpect(status().isBadRequest());
        cadastrar("{\"nome\":\"Família Souza\",\"quantidadePessoas\":\"duas\",\"convidados\":[\"Ana Souza\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("formato correto")));
        assertThat(familiaRepository.count()).isZero();
    }

    @Test
    void rejeitaQuantidadeIncompativelComOsNomes() throws Exception {
        cadastrar("{\"nome\":\"Família Souza\",\"quantidadePessoas\":3,\"convidados\":[\"Ana Souza\",\"João Souza\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("não corresponde")));
        assertThat(familiaRepository.count()).isZero();
    }

    @Test
    void rejeitaNomesDeParticipantesVazios() throws Exception {
        cadastrar("{\"nome\":\"Família Souza\",\"quantidadePessoas\":2,\"convidados\":[\"Ana Souza\",\"   \"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].mensagem").value("Preencha o nome completo de todos os participantes."));
        assertThat(familiaRepository.count()).isZero();
        assertThat(convidadoRepository.count()).isZero();
    }

    @Test
    void rejeitaQuantidadeAcimaDoLimiteConfigurado() throws Exception {
        StringBuilder nomes = new StringBuilder();
        for (int i = 1; i <= 31; i++) {
            nomes.append(i > 1 ? "," : "").append("\"Pessoa ").append(i).append('"');
        }
        cadastrar("{\"nome\":\"Família Grande\",\"quantidadePessoas\":31,\"convidados\":[" + nomes + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O limite é de 30 participantes por família."));
    }

    @Test
    void rejeitaJsonMalFormadoSemExporDetalhesInternos() throws Exception {
        cadastrar("{\"nome\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("Requisição inválida")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void mesmaChaveDeIdempotenciaNaoDuplicaCadastro() throws Exception {
        String chave = UUID.randomUUID().toString();
        String json = "{\"nome\":\"Família Lima\",\"quantidadePessoas\":2,\"convidados\":[\"Rita Lima\",\"Davi Lima\"]}";

        String primeira = mockMvc.perform(post("/api/public/familias").header("Idempotency-Key", chave)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String segunda = mockMvc.perform(post("/api/public/familias").header("Idempotency-Key", chave)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(segunda).isEqualTo(primeira);
        assertThat(familiaRepository.count()).isEqualTo(1);
        assertThat(convidadoRepository.count()).isEqualTo(2);
    }

    @Test
    void familiasDiferentesComMesmoSobrenomeSaoAceitas() throws Exception {
        cadastrarFamilia("Família Silva", "Carlos Silva");
        cadastrarFamilia("Família Silva", "Joana Silva");
        assertThat(familiaRepository.count()).isEqualTo(2);
    }

    @Test
    void chaveDeIdempotenciaInvalidaRetorna400() throws Exception {
        mockMvc.perform(post("/api/public/familias").header("Idempotency-Key", "nao-e-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Família X\",\"quantidadePessoas\":1,\"convidados\":[\"Ana X\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configuracaoPublicaInformaLimite() throws Exception {
        mockMvc.perform(get("/api/public/configuracao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxParticipantes").value(30));
    }
}
