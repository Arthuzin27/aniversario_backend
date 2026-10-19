package com.convite.evelyn.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Formato estável de paginação para o frontend (evita serializar PageImpl diretamente). */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<?> page, List<T> conteudo) {
        return new PaginaResponse<>(conteudo, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
