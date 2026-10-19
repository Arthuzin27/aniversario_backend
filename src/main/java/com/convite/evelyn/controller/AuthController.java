package com.convite.evelyn.controller;

import com.convite.evelyn.dto.LoginRequest;
import com.convite.evelyn.dto.LoginResponse;
import com.convite.evelyn.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login do painel administrativo")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o administrador e devolve um token Bearer")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.usuario(), request.senha()));
            log.info("Login administrativo realizado");
            return tokenService.emitir(auth.getName());
        } catch (AuthenticationException e) {
            log.warn("Tentativa de login administrativo inválida");
            throw new BadCredentialsException("Credenciais inválidas");
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o administrador do token atual")
    @SecurityRequirement(name = "bearer")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("usuario", authentication.getName());
    }
}
