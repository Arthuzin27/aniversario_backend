package com.convite.evelyn;

import com.convite.evelyn.repository.ConvidadoRepository;
import com.convite.evelyn.repository.FamiliaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "app.admin.usuario=" + IntegracaoTestBase.ADMIN_USUARIO,
        "app.admin.senha=" + IntegracaoTestBase.ADMIN_SENHA,
        "app.admin.redefinir-senha=false",
        "app.convite.max-participantes=30",
        "app.rate-limit.cadastros-por-minuto=10000",
        "app.rate-limit.logins-por-minuto=10000",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegracaoTestBase {

    protected static final String ADMIN_USUARIO = "admin-teste";
    protected static final String ADMIN_SENHA = "senha-de-teste-123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected FamiliaRepository familiaRepository;

    @Autowired
    protected ConvidadoRepository convidadoRepository;

    @BeforeEach
    void limparBanco() {
        convidadoRepository.deleteAllInBatch();
        familiaRepository.deleteAllInBatch();
    }

    protected String tokenAdmin() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + ADMIN_USUARIO + "\",\"senha\":\"" + ADMIN_SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    protected ResultActions cadastrar(String json) throws Exception {
        return mockMvc.perform(post("/api/public/familias").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Cadastra pelo endpoint público e devolve o ID criado. */
    protected long cadastrarFamilia(String nome, String... convidados) throws Exception {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < convidados.length; i++) {
            lista.append(i > 0 ? "," : "").append('"').append(convidados[i]).append('"');
        }
        String resposta = cadastrar("{\"nome\":\"" + nome + "\",\"quantidadePessoas\":" + convidados.length
                + ",\"convidados\":[" + lista + "]}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }
}
