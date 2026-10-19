package com.convite.evelyn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Edição administrativa de uma família. A lista enviada substitui a atual:
 * itens com {@code id} são atualizados, itens sem {@code id} são criados e
 * convidados ausentes da lista são removidos.
 */
public record FamiliaAtualizacaoRequest(
        @NotBlank(message = "Informe o nome da família.")
        @Size(max = 120, message = "O nome da família deve ter no máximo 120 caracteres.")
        String nome,

        @NotEmpty(message = "A família deve ter pelo menos um participante.")
        @Size(max = 100, message = "Quantidade de participantes acima do permitido.")
        List<@Valid @NotNull(message = "Participante inválido.") ConvidadoItem> convidados) {

    public record ConvidadoItem(
            @Schema(description = "ID do convidado existente; omita para adicionar um novo") Long id,
            @NotBlank(message = "Preencha o nome completo de todos os participantes.")
            @Size(max = 150, message = "Cada nome deve ter no máximo 150 caracteres.")
            String nomeCompleto) {
    }
}
