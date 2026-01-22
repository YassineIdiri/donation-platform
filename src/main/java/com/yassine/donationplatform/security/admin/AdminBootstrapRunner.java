package com.yassine.donationplatform.security.admin;

import com.yassine.donationplatform.user.User;
import com.yassine.donationplatform.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AdminProps props;

    public AdminBootstrapRunner(UserRepository users, PasswordEncoder encoder, AdminProps props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        String email = props.email();

        // ✅ En CI / env non configuré : on ne crash pas
        if (email == null || email.isBlank()) {
            System.out.println("[ADMIN BOOTSTRAP] Skipped (app.admin.email not set).");
            return;
        }

        // Si admin existe déjà -> rien
        if (users.existsByEmail(email)) return;

        String initialPassword = props.initialPassword();

        // ✅ Si pas de mot de passe fourni : on ne crash pas
        if (initialPassword == null || initialPassword.isBlank()) {
            System.out.println("[ADMIN BOOTSTRAP] Skipped (admin missing but initial password not set).");
            return;
        }

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(initialPassword));
        users.save(u);

        System.out.println("[ADMIN BOOTSTRAP] Admin user created: " + email);
        System.out.println("[ADMIN BOOTSTRAP] Please login and change password immediately.");
    }
}
