package com.convite.evelyn.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * Configurações da aplicação, lidas de application.properties / variáveis de ambiente.
 * A aplicação não inicia se alguma configuração obrigatória estiver ausente ou inválida.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Convite convite,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Admin admin,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Relatorio relatorio) {

    public record Convite(@DefaultValue("30") @Min(1) @Max(100) int maxParticipantes) {
    }

    public record Cors(@NotEmpty(message = "Defina CORS_ALLOWED_ORIGINS") List<String> allowedOrigins) {
    }

    public record Jwt(
            @NotBlank(message = "Defina JWT_SECRET (mínimo de 32 caracteres)")
            @Size(min = 32, message = "JWT_SECRET deve ter no mínimo 32 caracteres") String secret,
            @DefaultValue("8h") @NotNull Duration expiracao) {
    }

    public record Admin(String usuario, String senha, boolean redefinirSenha) {
    }

    public record RateLimit(
            @DefaultValue("10") @Min(1) int cadastrosPorMinuto,
            @DefaultValue("5") @Min(1) int loginsPorMinuto) {
    }

    public record Relatorio(@DefaultValue("America/Sao_Paulo") @NotNull ZoneId fusoHorario) {
    }
}
