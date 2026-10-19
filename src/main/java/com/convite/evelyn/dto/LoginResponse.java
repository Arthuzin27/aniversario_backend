package com.convite.evelyn.dto;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEm, String usuario) {
}
