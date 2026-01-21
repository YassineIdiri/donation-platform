package com.yassine.donationplatform.security.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminProps {
    private final String email;
    private final String initialPassword;
    private final String resetPassword;
    private final String supportResetKey;

    public AdminProps(
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.initial-password:}") String initialPassword,
            @Value("${app.admin.reset-password:}") String resetPassword,
            @Value("${app.admin.support-reset-key:}") String supportResetKey
    ) {
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.initialPassword = initialPassword == null ? "" : initialPassword;
        this.resetPassword = resetPassword == null ? "" : resetPassword;
        this.supportResetKey = supportResetKey == null ? "" : supportResetKey;
    }

    public String email() { return email; }
    public String initialPassword() { return initialPassword; }
    public String resetPassword() { return resetPassword; }
    public String supportResetKey() { return supportResetKey; }
}
