// src/main/java/com/yassine/donationplatform/web/admin/AdminAccountController.java
package com.yassine.donationplatform.web.admin;

import com.yassine.donationplatform.dto.request.AdminChangePasswordRequest;
import com.yassine.donationplatform.dto.request.AdminSupportResetRequest;
import com.yassine.donationplatform.security.auth.AdminPasswordService;
import com.yassine.donationplatform.security.auth.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/account")
public class AdminAccountController {

    private final AdminPasswordService passwords;

    public AdminAccountController(AdminPasswordService passwords) {
        this.passwords = passwords;
    }

    // ✅ admin connecté
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthUser admin,
            @Valid @RequestBody AdminChangePasswordRequest req
    ) {
        passwords.changePassword(admin, req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    // ✅ support (toi) : clé obligatoire
    @PostMapping("/support-reset-password")
    public ResponseEntity<Void> supportReset(
            @RequestHeader(name = "X-ADMIN-RESET-KEY", required = false) String resetKey,
            @RequestBody(required = false) AdminSupportResetRequest body
    ) {
        String newPassword = (body == null) ? null : body.newPassword();
        passwords.supportReset(resetKey, newPassword);
        return ResponseEntity.noContent().build();
    }
}
