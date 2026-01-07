package com.yassine.smartexpensetracker.auth.reset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetDtos {

    public record ForgotPasswordRequest(
            @Email @NotBlank String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    public record OkResponse(boolean ok) {}
}
