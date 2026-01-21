package com.yassine.donationplatform.security.auth;

import com.yassine.donationplatform.security.admin.AdminProps;
import com.yassine.donationplatform.user.User;
import com.yassine.donationplatform.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPasswordService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AdminProps adminProps;

    public AdminPasswordService(UserRepository users, PasswordEncoder encoder, AdminProps adminProps) {
        this.users = users;
        this.encoder = encoder;
        this.adminProps = adminProps;
    }

    /** L'admin connecté change son mdp */
    @Transactional
    public void changePassword(AuthUser admin, String currentPassword, String newPassword) {
        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User u = users.findById(admin.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (isBlank(currentPassword) || isBlank(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing password");
        }
        if (newPassword.length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password too short (min 10)");
        }

        if (!encoder.matches(currentPassword, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }

        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
    }

    /** Support reset (toi) : protégé par une clé header + password venant soit du body soit de ADMIN_RESET_PASSWORD */
    @Transactional
    public void supportReset(String providedKey, String bodyPasswordOrNull) {
        String expectedKey = adminProps.supportResetKey();
        if (isBlank(expectedKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Support reset not configured");
        }
        if (isBlank(providedKey) || !constantTimeEquals(providedKey, expectedKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        String email = adminProps.email();
        if (isBlank(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin email not configured");
        }

        User u = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        // mot de passe du body prioritaire, sinon env ADMIN_RESET_PASSWORD
        String newPass = !isBlank(bodyPasswordOrNull) ? bodyPasswordOrNull : adminProps.resetPassword();
        if (isBlank(newPass)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing new password (body or ADMIN_RESET_PASSWORD)");
        }
        if (newPass.length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password too short (min 10)");
        }

        u.setPasswordHash(encoder.encode(newPass));
        users.save(u);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // comparaison "constant time" pour éviter timing attacks (simple mais mieux que equals)
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] y = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int diff = x.length ^ y.length;
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            diff |= x[i] ^ y[i];
        }
        return diff == 0;
    }
}
