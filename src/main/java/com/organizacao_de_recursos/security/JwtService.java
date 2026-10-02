package com.organizacao_de_recursos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Emite e valida os tokens JWT da API stateless (/api/**, ADR-018). */
@Service
public class JwtService {

    private final SecretKey chave;
    private final Duration validade;
    private final Clock clock;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                       @Value("${app.security.jwt.validade-minutos:120}") long validadeMinutos,
                       Clock clock) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validade = Duration.ofMinutes(validadeMinutos);
        this.clock = clock;
    }

    public String gerarToken(String username, String perfil) {
        Instant agora = clock.instant();
        return Jwts.builder()
                .subject(username)
                .claim("perfil", perfil)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    /** @return o username (subject) do token, ou vazio se inválido/expirado. */
    public java.util.Optional<String> validarEExtrairUsername(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return java.util.Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException tokenInvalido) {
            return java.util.Optional.empty();
        }
    }
}
