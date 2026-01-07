package com.yassine.smartexpensetracker.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes).");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** TTL explicite (seconds) */
    public String generateToken(UUID userId, String email, long ttlSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email) // optionnel
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public UUID parseUserId(String token) {
        String sub = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return UUID.fromString(sub);
    }
}
