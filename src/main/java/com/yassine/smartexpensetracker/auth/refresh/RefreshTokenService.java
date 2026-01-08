package com.yassine.smartexpensetracker.auth.refresh;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Duration REFRESH_TTL_DEFAULT = Duration.ofDays(1);
    private static final Duration REFRESH_TTL_REMEMBER = Duration.ofDays(30);

    private final RefreshTokenRepository repo;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public IssueResult issue(UUID userId, boolean rememberMe) {
        String raw = generateRawToken();
        String hash = sha256Hex(raw);

        Instant expiresAt = Instant.now().plus(
                rememberMe ? REFRESH_TTL_REMEMBER : REFRESH_TTL_DEFAULT
        );

        RefreshToken rt = new RefreshToken(hash, userId, expiresAt);
        repo.save(rt);

        return new IssueResult(raw, expiresAt);
    }


    /**
     * Vérifie et ROTATE un refresh token brut.
     * - Révoque l'ancien
     * - Crée un nouveau refresh token qui expire à la MÊME date que l'ancien
     * Retourne userId + nouveau token brut.
     */
    @Transactional
    public RotationResult verifyAndRotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }

        String hash = sha256Hex(rawToken);
        RefreshToken existing = repo.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        if (existing.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token revoked");
        }
        if (existing.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // Révoque l'ancien
        existing.revoke();
        repo.save(existing);

        // Crée un nouveau qui expire à la même date
        String newRaw = generateRawToken();
        String newHash = sha256Hex(newRaw);

        RefreshToken rotated = new RefreshToken(newHash, existing.getUserId(), existing.getExpiresAt());
        repo.save(rotated);

        return new RotationResult(existing.getUserId(), newRaw, existing.getExpiresAt());
    }

    /** Révoque le refresh token brut (logout). Idempotent. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;

        String hash = sha256Hex(rawToken);
        repo.findByTokenHash(hash).ifPresent(rt -> {
            if (!rt.isRevoked()) {
                rt.revoke();
                repo.save(rt);
            }
        });
    }

    @Transactional
    public int revokeAllForUser(UUID userId) {
        return repo.revokeAllActiveByUserId(userId, Instant.now());
    }

    // helpers
    private String generateRawToken() {
        byte[] bytes = new byte[32]; // 256-bit
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public record RotationResult(UUID userId, String newRawRefreshToken, Instant expiresAt) {}

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) {
            super(message);
        }
    }

    public record IssueResult(String rawRefreshToken, Instant expiresAt) {}


}
