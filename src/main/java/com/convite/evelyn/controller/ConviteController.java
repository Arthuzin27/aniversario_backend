package com.convite.evelyn.controller;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.dto.CadastroConfirmadoResponse;
import com.convite.evelyn.dto.ConfiguracaoPublicaResponse;
import com.convite.evelyn.dto.FamiliaCadastroRequest;
import com.convite.evelyn.service.FamiliaService;
import com.convite.evelyn.service.FamiliaService.ResultadoCadastro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Convite (público)", description = "Confirmação de presença pelo link do convite")
public class ConviteController {

    private final FamiliaService familiaService;
    private final AppProperties properties;

    public ConviteController(FamiliaService familiaService, AppProperties properties) {
        this.familiaService = familiaService;
        this.properties = properties;
    }

    @PostMapping("/familias")
    @Operation(summary = "Confirma a presença de uma família e seus participantes",
            description = "Retorna 201 quando criado. Reenviar com o mesmo Idempotency-Key devolve 200 com o cadastro original, sem duplicar.")
    public ResponseEntity<CadastroConfirmadoResponse> cadastrar(
            @Parameter(description = "UUID gerado pelo formulário para evitar envios duplicados")
            @RequestHeader(name = "Idempotency-Key", required = false) UUID chaveIdempotencia,
            @Valid @RequestBody FamiliaCadastroRequest request) {
        ResultadoCadastro resultado = familiaService.cadastrar(request, chaveIdempotencia);
        return ResponseEntity.status(resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.resposta());
    }

    @GetMapping("/configuracao")
    @Operation(summary = "Configurações necessárias para montar o formulário")
    public ConfiguracaoPublicaResponse configuracao() {
        return new ConfiguracaoPublicaResponse(properties.convite().maxParticipantes());
    }
}
