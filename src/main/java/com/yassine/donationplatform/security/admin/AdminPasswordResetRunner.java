// src/main/java/com/yassine/donationplatform/security/admin/AdminPasswordResetRunner.java
package com.yassine.donationplatform.security.admin;

import com.yassine.donationplatform.security.refresh.RefreshTokenService;
import com.yassine.donationplatform.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminPasswordResetRunner implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshTokens;
    private final AdminProps props;

    public AdminPasswordResetRunner(UserRepository users,
                                    PasswordEncoder encoder,
                                    RefreshTokenService refreshTokens,
                                    AdminProps props) {
        this.users = users;
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        String reset = props.resetPassword();
        if (reset.isBlank()) return; // pas de reset demandé

        String email = props.email();
        var user = users.findByEmail(email).orElseThrow(() ->
                new IllegalStateException("Admin user not found for reset: " + email));

        user.setPasswordHash(encoder.encode(reset));
        users.save(user);

        // Important : invalider toutes les sessions
        refreshTokens.revokeAllForUser(user.getId());

        System.out.println("[ADMIN RESET] Password updated for " + email);
        System.out.println("[ADMIN RESET] Remove ADMIN_RESET_PASSWORD after reset.");
    }
}
