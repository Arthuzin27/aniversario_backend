package com.convite.evelyn.security;

import com.convite.evelyn.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita, por IP, o número de envios do formulário público e de tentativas de login
 * em uma janela fixa de 1 minuto. Mantido em memória: suficiente para uma única instância
 * do backend, sem exigir Redis ou outra infraestrutura.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long JANELA_MS = 60_000;
    private static final int LIMPEZA_ACIMA_DE = 10_000;

    private final AppProperties properties;
    private final Clock clock;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(AppProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimitFilter(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    private record Janela(long inicio, int contagem) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitePara(request) == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limite = limitePara(request);
        String chave = request.getRequestURI() + "|" + request.getRemoteAddr();
        long agora = clock.millis();

        if (janelas.size() > LIMPEZA_ACIMA_DE) {
            janelas.values().removeIf(j -> agora - j.inicio() >= JANELA_MS);
        }
        Janela janela = janelas.compute(chave, (k, atual) ->
                atual == null || agora - atual.inicio() >= JANELA_MS
                        ? new Janela(agora, 1)
                        : new Janela(atual.inicio(), atual.contagem() + 1));

        if (janela.contagem() > limite) {
            long segundos = Math.max(1, (JANELA_MS - (agora - janela.inicio()) + 999) / 1000);
            log.warn("Limite de requisições excedido em {}", request.getRequestURI());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
            RespostaJson.escrever(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas em pouco tempo. Aguarde um minuto e tente novamente.", request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }

    private int limitePara(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return 0;
        }
        return switch (request.getRequestURI()) {
            case "/api/public/familias" -> properties.rateLimit().cadastrosPorMinuto();
            case "/api/auth/login" -> properties.rateLimit().loginsPorMinuto();
            default -> 0;
        };
    }
}
