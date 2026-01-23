package com.yassine.donationplatform.controller;

import com.yassine.donationplatform.dto.request.AdminChangePasswordRequest;
import com.yassine.donationplatform.service.auth.AdminAuthService;
import com.yassine.donationplatform.dto.AuthUser;
import com.yassine.donationplatform.dto.request.AdminLoginRequest;
import com.yassine.donationplatform.dto.response.AuthTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService auth;

    public AdminAuthController(AdminAuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AdminLoginRequest req,
                                                   HttpServletRequest httpReq) {
        var result = auth.login(req, httpReq);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
            @CookieValue(name = "admin_refresh", required = false) String refreshToken,
            HttpServletRequest httpReq
    ) {
        var result = auth.refresh(refreshToken, httpReq);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "admin_refresh", required = false) String refreshToken
    ) {
        var clear = auth.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody AdminChangePasswordRequest req) {
        var clear = auth.changePassword(user.id(), req);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .build();
    }
}
