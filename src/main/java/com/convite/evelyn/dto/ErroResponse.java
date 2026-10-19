package com.convite.evelyn.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Formato padronizado de erro devolvido por toda a API. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> campos) {

    public record CampoErro(String campo, String mensagem) {
    }
}
