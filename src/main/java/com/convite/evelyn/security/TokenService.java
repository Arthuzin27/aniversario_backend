package com.convite.evelyn.security;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.dto.LoginResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Emite tokens JWT assinados (HS256) para administradores autenticados. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final AppProperties properties;

    public TokenService(JwtEncoder encoder, AppProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public LoginResponse emitir(String usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(properties.jwt().expiracao());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("evelyn")
                .subject(usuario)
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("scope", "ADMIN")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiraEm, usuario);
    }
}
