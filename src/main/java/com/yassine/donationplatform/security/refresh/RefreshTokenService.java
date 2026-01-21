package com.yassine.donationplatform.security.refresh;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    // Ajuste selon UX
    private static final Duration TTL_SHORT = Duration.ofDays(7);
    private static final Duration TTL_LONG  = Duration.ofDays(30);

    private final RefreshTokenRepository repo;
    private final SecureRandom rng = new SecureRandom();
    private final String pepper; // optionnel

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${app.auth.refresh-pepper:}") String pepper) {
        this.repo = repo;
        this.pepper = pepper == null ? "" : pepper;
    }

    public record IssueResult(String rawToken, Instant expiresAt) {}
    public record RotationResult(UUID userId, String newRawToken, Instant newExpiresAt) {}

    @Transactional
    public IssueResult issue(UUID userId, boolean rememberMe, HttpServletRequest req) {
        Instant expiresAt = Instant.now().plus(rememberMe ? TTL_LONG : TTL_SHORT);

        String raw = generateRawToken();
        String hash = sha256Hex(raw);

        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .userAgent(safe(req.getHeader("User-Agent")))
                .ip(safeClientIp(req))
                .build();

        repo.save(rt);
        return new IssueResult(raw, expiresAt);
    }

    @Transactional
    public RotationResult verifyAndRotate(String rawToken, boolean rememberMe, HttpServletRequest req) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        String hash = sha256Hex(rawToken);
        RefreshToken current = repo.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (current.isRevoked() || current.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired/revoked");
        }

        // rotation
        IssueResult issued = issue(current.getUserId(), rememberMe, req);

        // marquer l'ancien comme révoqué + replacedBy (optionnel)
        current.setRevokedAt(Instant.now());
        // on ne connaît pas l'UUID du nouveau sans le recharger; on peut laisser null, ou faire un find par hash.
        repo.save(current);

        return new RotationResult(current.getUserId(), issued.rawToken(), issued.expiresAt());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;

        String hash = sha256Hex(rawToken);
        repo.findByTokenHash(hash).ifPresent(rt -> {
            if (!rt.isRevoked()) {
                rt.setRevokedAt(Instant.now());
                repo.save(rt);
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        // simple: supprimer tout (ou marquer revoked)
        repo.deleteByUserId(userId);
    }

    @Transactional
    public long cleanupExpired() {
        return repo.deleteByExpiresAtBefore(Instant.now());
    }

    // -------- helpers --------

    private String generateRawToken() {
        byte[] bytes = new byte[48]; // 384 bits
        rng.nextBytes(bytes);
        // base64url sans padding
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String material = raw + pepper;
            byte[] digest = md.digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String safe(String s) {
        return s == null ? null : s.trim();
    }

    private static String safeClientIp(HttpServletRequest req) {
        // simple (tu peux améliorer si tu as proxy)
        return req.getRemoteAddr();
    }
}
