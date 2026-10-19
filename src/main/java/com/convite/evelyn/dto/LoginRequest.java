package com.convite.evelyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o usuário.") @Size(max = 60, message = "Usuário inválido.") String usuario,
        @NotBlank(message = "Informe a senha.") @Size(max = 128, message = "Senha inválida.") String senha) {

    @Override
    public String toString() {
        return "LoginRequest[usuario=" + usuario + ", senha=***]";
    }
}
