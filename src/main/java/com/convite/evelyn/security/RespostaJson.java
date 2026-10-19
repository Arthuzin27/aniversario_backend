package com.convite.evelyn.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Escreve erros no mesmo formato de {@code ErroResponse} para respostas geradas fora dos controllers (filtros). */
final class RespostaJson {

    private RespostaJson() {
    }

    static void escrever(HttpServletResponse response, HttpStatus status, String mensagem, String caminho) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
                + ",\"erro\":\"" + status.getReasonPhrase() + "\",\"mensagem\":\"" + escapar(mensagem)
                + "\",\"caminho\":\"" + escapar(caminho) + "\"}");
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
