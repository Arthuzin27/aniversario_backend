package com.convite.evelyn.dto;

/** Projeção usada pelo relatório: convidado com o nome da família, sem carregar entidades. */
public record ConvidadoComFamilia(Long id, String nomeCompleto, String familia) {
}
