package com.convite.evelyn.dto;

import java.util.List;

/** Resposta do cadastro público: confirma o que foi efetivamente salvo. */
public record CadastroConfirmadoResponse(
        Long id,
        String nome,
        int quantidadePessoas,
        List<String> convidados,
        String mensagem) {
}
