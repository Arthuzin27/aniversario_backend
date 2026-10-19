package com.convite.evelyn.security;

import com.convite.evelyn.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-01-01T12:00:00Z"));
    private final Clock relogio = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora.get();
        }
    };

    private final RateLimitFilter filtro = new RateLimitFilter(new AppProperties(
            new AppProperties.Convite(30),
            new AppProperties.Cors(List.of("http://localhost")),
            new AppProperties.Jwt("x".repeat(32), Duration.ofHours(1)),
            new AppProperties.Admin(null, null, false),
            new AppProperties.RateLimit(2, 1),
            new AppProperties.Relatorio(ZoneId.of("UTC"))), relogio);

    @Test
    void bloqueiaAcimaDoLimiteELiberaNaProximaJanela() throws Exception {
        assertThat(enviar("POST", "/api/public/familias", "10.0.0.1")).isEqualTo(200);
        assertThat(enviar("POST", "/api/public/familias", "10.0.0.1")).isEqualTo(200);
        MockHttpServletResponse bloqueada = executar("POST", "/api/public/familias", "10.0.0.1");
        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isNotBlank();
        assertThat(bloqueada.getContentAsString()).contains("Aguarde um minuto");

        // outro IP não é afetado
        assertThat(enviar("POST", "/api/public/familias", "10.0.0.2")).isEqualTo(200);

        agora.set(agora.get().plusSeconds(61));
        assertThat(enviar("POST", "/api/public/familias", "10.0.0.1")).isEqualTo(200);
    }

    @Test
    void loginTemLimiteProprioEOutrasRotasNaoSaoLimitadas() throws Exception {
        assertThat(enviar("POST", "/api/auth/login", "10.0.0.3")).isEqualTo(200);
        assertThat(enviar("POST", "/api/auth/login", "10.0.0.3")).isEqualTo(429);
        for (int i = 0; i < 5; i++) {
            assertThat(enviar("GET", "/api/admin/familias", "10.0.0.3")).isEqualTo(200);
        }
    }

    private int enviar(String metodo, String uri, String ip) throws Exception {
        return executar(metodo, uri, ip).getStatus();
    }

    private MockHttpServletResponse executar(String metodo, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
