package com.yassine.donationplatform.security.admin;

import com.yassine.donationplatform.service.auth.RefreshTokenService;
import com.yassine.donationplatform.repository.UserRepository;
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
        if (reset == null || reset.isBlank()) return; // pas de reset demandé

        String email = props.email();
        if (email == null || email.isBlank()) {
            System.out.println("[ADMIN RESET] Skipped (app.admin.email not set).");
            return;
        }

        var userOpt = users.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("[ADMIN RESET] Skipped (admin user not found: " + email + ").");
            return;
        }

        var user = userOpt.get();
        user.setPasswordHash(encoder.encode(reset));
        users.save(user);

        refreshTokens.revokeAllForUser(user.getId());

        System.out.println("[ADMIN RESET] Password updated for " + email);
        System.out.println("[ADMIN RESET] Remove ADMIN_RESET_PASSWORD after reset.");
    }
}
