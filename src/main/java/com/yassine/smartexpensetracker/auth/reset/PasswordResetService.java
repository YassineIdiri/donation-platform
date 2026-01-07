package com.yassine.smartexpensetracker.auth.reset;

import com.yassine.smartexpensetracker.user.User;
import com.yassine.smartexpensetracker.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    private final Duration resetTtl;
    private final String frontendBaseUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            @Value("${app.password-reset.ttl-minutes:30}") long ttlMinutes,
            @Value("${app.frontend.base-url:http://localhost:4200}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.resetTtl = Duration.ofMinutes(ttlMinutes);
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * IMPORTANT: toujours répondre OK même si l’email n’existe pas
     * (anti user-enumeration).
     */
    @Transactional
    public void forgotPassword(String emailRaw) {
        String email = emailRaw.trim().toLowerCase();

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return; // on fait comme si c'était OK
        }

        User user = userOpt.get();

        // 1) Générer token sécurisé (random)
        String token = generateSecureToken();

        // 2) Stocker un hash du token en DB
        String tokenHash = sha256Base64(token);
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash(tokenHash);
        prt.setExpiresAt(Instant.now().plus(resetTtl));
        tokenRepository.save(prt);

        // 3) Envoyer email avec lien vers front
        String link = frontendBaseUrl + "/reset-password?token=" + urlEncode(token);

        String html = """
                <p>You requested a password reset.</p>
                <p>Click this link to reset your password (valid %d minutes):</p>
                <p><a href="%s">%s</a></p>
                <p>If you didn’t request this, you can ignore this email.</p>
                """.formatted(resetTtl.toMinutes(), link, link);

        emailSender.send(user.getEmail(), "Reset your password", html);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = sha256Base64(token);

        PasswordResetToken prt = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (prt.isUsed() || prt.isExpired()) {
            throw new IllegalArgumentException("Token expired or already used");
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsedAt(Instant.now());
        tokenRepository.save(prt);

        // OPTION RECOMMANDÉE:
        // invalider tous les refresh tokens de l’utilisateur (s’il y a une table refresh_tokens)
        // refreshTokenRepository.deleteByUserId(user.getId());
    }

    // ---------- utils ----------

    private static final SecureRandom RNG = new SecureRandom();

    private static String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Base64(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
