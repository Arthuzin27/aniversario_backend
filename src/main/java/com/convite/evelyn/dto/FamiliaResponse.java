package com.convite.evelyn.dto;

import java.time.Instant;
import java.util.List;

public record FamiliaResponse(
        Long id,
        String nome,
        int quantidadePessoas,
        List<ConvidadoResponse> convidados,
        Instant dataCadastro,
        Instant dataAtualizacao,
        boolean possivelDuplicata) {

    public record ConvidadoResponse(Long id, String nomeCompleto) {
    }
}
