package com.convite.evelyn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Dados enviados pelo formulário público de confirmação de presença. */
public record FamiliaCadastroRequest(
        @Schema(example = "Família Silva")
        @NotBlank(message = "Informe o nome da família.")
        @Size(max = 120, message = "O nome da família deve ter no máximo 120 caracteres.")
        String nome,

        @Schema(example = "2")
        @NotNull(message = "Informe a quantidade de pessoas.")
        @Positive(message = "A quantidade de pessoas deve ser um número inteiro positivo.")
        Integer quantidadePessoas,

        @Schema(example = "[\"Ana Silva\", \"Carlos Silva\"]")
        @NotNull(message = "Informe o nome de cada participante.")
        @Size(max = 100, message = "Quantidade de participantes acima do permitido.")
        List<
                @NotBlank(message = "Preencha o nome completo de todos os participantes.")
                @Size(max = 150, message = "Cada nome deve ter no máximo 150 caracteres.")
                String> convidados) {
}
