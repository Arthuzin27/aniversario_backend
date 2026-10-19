package com.convite.evelyn.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RelatorioConvidadosResponse(
        String titulo,
        OffsetDateTime geradoEm,
        List<Item> convidados,
        int totalPessoas) {

    public record Item(int numero, String nomeCompleto, String familia) {
    }
}
