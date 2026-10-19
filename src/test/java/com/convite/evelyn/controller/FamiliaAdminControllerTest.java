package com.convite.evelyn.controller;

import com.convite.evelyn.IntegracaoTestBase;
import com.convite.evelyn.entity.Convidado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FamiliaAdminControllerTest extends IntegracaoTestBase {

    private String token;

    @BeforeEach
    void autenticar() throws Exception {
        token = tokenAdmin();
    }

    @Test
    void endpointsAdministrativosExigemAutenticacao() throws Exception {
        for (String url : List.of("/api/admin/familias", "/api/admin/familias/1", "/api/admin/estatisticas",
                "/api/admin/relatorios/convidados", "/api/admin/relatorios/convidados/pdf",
                "/api/admin/relatorios/convidados/csv", "/api/auth/me")) {
            mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensagem").value("Autenticação necessária. Faça login novamente."));
        }
        mockMvc.perform(delete("/api/admin/familias/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/familias/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoOuAdulteradoERejeitado() throws Exception {
        mockMvc.perform(get("/api/admin/familias").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/familias").header(HttpHeaders.AUTHORIZATION, token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComSenhaErradaRetorna401SemDetalhes() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + ADMIN_USUARIO + "\",\"senha\":\"senha-errada-000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos."));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"inexistente\",\"senha\":\"qualquer-coisa\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos."));
    }

    @Test
    void tokenValidoDaAcessoAoPerfil() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value(ADMIN_USUARIO));
    }

    @Test
    void listaComParticipantesBuscaEPaginacao() throws Exception {
        cadastrarFamilia("Família Silva", "Carlos Silva", "Ana Silva");
        cadastrarFamilia("Família Souza", "Ana Souza");
        cadastrarFamilia("Família Lima", "Rita Lima");

        mockMvc.perform(get("/api/admin/familias").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Família Lima"))
                .andExpect(jsonPath("$.conteudo[2].quantidadePessoas").value(2))
                .andExpect(jsonPath("$.conteudo[2].convidados[*].nomeCompleto", contains("Carlos Silva", "Ana Silva")));

        // busca por nome de participante (sem diferenciar maiúsculas)
        mockMvc.perform(get("/api/admin/familias").param("busca", "ANA").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElementos").value(2));
        // busca por nome da família
        mockMvc.perform(get("/api/admin/familias").param("busca", "lima").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Família Lima"));
        // curingas digitados pelo usuário não são interpretados
        mockMvc.perform(get("/api/admin/familias").param("busca", "%").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElementos").value(0));
        // paginação
        mockMvc.perform(get("/api/admin/familias").param("pagina", "1").param("tamanho", "2")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mockMvc.perform(get("/api/admin/familias").param("tamanho", "500").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinalizaPossivelDuplicidadePorNomeIgual() throws Exception {
        cadastrarFamilia("Família Costa", "Leo Costa");
        cadastrarFamilia("família costa", "Leo Costa");
        cadastrarFamilia("Família Rocha", "Bia Rocha");

        mockMvc.perform(get("/api/admin/familias").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.conteudo[?(@.nome == 'Família Rocha')].possivelDuplicata", contains(false)))
                .andExpect(jsonPath("$.conteudo[?(@.nome =~ /(?i)família costa/)].possivelDuplicata", contains(true, true)));
    }

    @Test
    void consultaPorIdEFamiliaInexistente() throws Exception {
        long id = cadastrarFamilia("Família Souza", "Ana Souza");
        mockMvc.perform(get("/api/admin/familias/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convidados[0].nomeCompleto").value("Ana Souza"));
        mockMvc.perform(get("/api/admin/familias/999999").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Família não encontrada."));
        mockMvc.perform(get("/api/admin/familias/abc").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editaNomeAdicionaAtualizaERemoveParticipantes() throws Exception {
        long id = cadastrarFamilia("Familia Olivera", "Maria Oliveira", "Bruno Oliveira", "Remover Oliveira");
        List<Convidado> atuais = convidadoRepository.findByFamiliaIds(List.of(id));
        long maria = atuais.get(0).getId();
        long bruno = atuais.get(1).getId();

        String json = """
                {"nome":"  Família Oliveira ","convidados":[
                  {"id":%d,"nomeCompleto":"Maria  Oliveira"},
                  {"id":%d,"nomeCompleto":"Bruno Oliveira Santos"},
                  {"nomeCompleto":"Pedro Oliveira"}
                ]}""".formatted(maria, bruno);
        mockMvc.perform(put("/api/admin/familias/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Família Oliveira"))
                .andExpect(jsonPath("$.quantidadePessoas").value(3))
                .andExpect(jsonPath("$.convidados[*].nomeCompleto",
                        contains("Maria Oliveira", "Bruno Oliveira Santos", "Pedro Oliveira")));

        List<Convidado> depois = convidadoRepository.findByFamiliaIds(List.of(id));
        assertThat(depois).hasSize(3);
        assertThat(depois.get(0).getId()).isEqualTo(maria);
        assertThat(convidadoRepository.count()).isEqualTo(3);
    }

    @Test
    void edicaoRejeitaParticipanteDeOutraFamiliaEListaVazia() throws Exception {
        long silva = cadastrarFamilia("Família Silva", "Carlos Silva");
        long souza = cadastrarFamilia("Família Souza", "Ana Souza");
        long idAna = convidadoRepository.findByFamiliaIds(List.of(souza)).get(0).getId();

        mockMvc.perform(put("/api/admin/familias/" + silva).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Família Silva\",\"convidados\":[{\"id\":" + idAna + ",\"nomeCompleto\":\"Ana\"}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/familias/" + silva).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Família Silva\",\"convidados\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].mensagem").value("A família deve ter pelo menos um participante."));
        mockMvc.perform(put("/api/admin/familias/999999").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"convidados\":[{\"nomeCompleto\":\"Y\"}]}"))
                .andExpect(status().isNotFound());

        assertThat(convidadoRepository.findByFamiliaIds(List.of(souza))).extracting(Convidado::getNomeCompleto)
                .containsExactly("Ana Souza");
    }

    @Test
    void excluiFamiliaEConvidados() throws Exception {
        long id = cadastrarFamilia("Família Lima", "Rita Lima", "Davi Lima");
        cadastrarFamilia("Família Souza", "Ana Souza");

        mockMvc.perform(delete("/api/admin/familias/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/familias/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());

        assertThat(familiaRepository.count()).isEqualTo(1);
        assertThat(convidadoRepository.count()).isEqualTo(1);
    }

    @Test
    void estatisticasRefletemOsConvidadosDoBanco() throws Exception {
        mockMvc.perform(get("/api/admin/estatisticas").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalPessoas").value(0))
                .andExpect(jsonPath("$.totalFamilias").value(0))
                .andExpect(jsonPath("$.mediaPessoasPorFamilia").value(0));

        cadastrarFamilia("Família Silva", "Carlos Silva", "Ana Silva", "Pedro Silva");
        long souza = cadastrarFamilia("Família Souza", "Ana Souza");
        cadastrarFamilia("Família Lima", "Rita Lima", "Davi Lima");

        mockMvc.perform(get("/api/admin/estatisticas").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalPessoas").value(6))
                .andExpect(jsonPath("$.totalFamilias").value(3))
                .andExpect(jsonPath("$.mediaPessoasPorFamilia").value(2.0));

        mockMvc.perform(delete("/api/admin/familias/" + souza).header(HttpHeaders.AUTHORIZATION, token));
        mockMvc.perform(get("/api/admin/estatisticas").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalPessoas").value(5))
                .andExpect(jsonPath("$.totalFamilias").value(2))
                .andExpect(jsonPath("$.mediaPessoasPorFamilia").value(2.5));
    }

    @Test
    void rotaInexistenteRetornaErroPadronizado() throws Exception {
        mockMvc.perform(get("/api/admin/nao-existe").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
