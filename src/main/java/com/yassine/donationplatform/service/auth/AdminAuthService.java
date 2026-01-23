package com.yassine.donationplatform.service.auth;

import com.yassine.donationplatform.dto.request.AdminChangePasswordRequest;
import com.yassine.donationplatform.dto.request.AdminLoginRequest;
import com.yassine.donationplatform.dto.response.AuthTokenResponse;
import com.yassine.donationplatform.security.admin.AdminProps;
import com.yassine.donationplatform.security.refresh.AuthCookieProps;
import com.yassine.donationplatform.entity.auth.User;
import com.yassine.donationplatform.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminAuthService {

    public static final long ACCESS_TTL_SECONDS = 15 * 60; // 15 min

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final AuthCookieProps cookieProps;
    private final AdminProps adminProps;

    public record AuthResult(AuthTokenResponse response, ResponseCookie refreshCookie) {}

    public AdminAuthService(UserRepository users,
                            PasswordEncoder encoder,
                            JwtService jwt,
                            RefreshTokenService refreshTokens,
                            AuthCookieProps cookieProps,
                            AdminProps adminProps) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.cookieProps = cookieProps;
        this.adminProps = adminProps;
    }

    @Transactional(readOnly = true)
    public AuthResult login(AdminLoginRequest req, HttpServletRequest httpReq) {
        String email = normalizeEmail(req.email());
        String password = req.password() == null ? "" : req.password();

        if (!email.equals(adminProps.email())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }

        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }

        String access = jwt.generateToken(user.getId(), user.getEmail(), ACCESS_TTL_SECONDS);
        AuthTokenResponse payload = new AuthTokenResponse(access, ACCESS_TTL_SECONDS);

        boolean rememberMe = true;
        var issued = refreshTokens.issue(user.getId(), rememberMe, httpReq);

        ResponseCookie cookie = buildRefreshCookie(issued.rawToken(), issued.expiresAt());
        return new AuthResult(payload, cookie);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, HttpServletRequest httpReq) {
        boolean rememberMe = true;
        var rotated = refreshTokens.verifyAndRotate(rawRefreshToken, rememberMe, httpReq);

        User user = users.findById(rotated.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // admin unique : protège aussi refresh
        if (!user.getEmail().equalsIgnoreCase(adminProps.email())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not allowed");
        }

        String access = jwt.generateToken(user.getId(), user.getEmail(), ACCESS_TTL_SECONDS);
        AuthTokenResponse payload = new AuthTokenResponse(access, ACCESS_TTL_SECONDS);

        ResponseCookie cookie = buildRefreshCookie(rotated.newRawToken(), rotated.newExpiresAt());
        return new AuthResult(payload, cookie);
    }

    @Transactional
    public ResponseCookie logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
        return clearRefreshCookie();
    }

    @Transactional
    public ResponseCookie changePassword(UUID userId, AdminChangePasswordRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.getEmail().equalsIgnoreCase(adminProps.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid current password");
        }

        user.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(user);

        refreshTokens.revokeAllForUser(user.getId());

        return clearRefreshCookie();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(cookieProps.refreshCookieName, "")
                .httpOnly(true)
                .secure(cookieProps.cookieSecure)
                .sameSite(cookieProps.sameSite)
                .path(cookieProps.refreshCookiePath)
                .maxAge(0)
                .build();
    }

    private ResponseCookie buildRefreshCookie(String rawRefreshToken, Instant expiresAt) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        return ResponseCookie.from(cookieProps.refreshCookieName, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieProps.cookieSecure)
                .sameSite(cookieProps.sameSite)
                .path(cookieProps.refreshCookiePath)
                .maxAge(maxAge)
                .build();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
