package com.yassine.smartexpensetracker.auth.reset;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.yassine.smartexpensetracker.auth.reset.PasswordResetDtos.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/forgot-password")
    public OkResponse forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        service.forgotPassword(req.email());
        return new OkResponse(true);
    }

    @PostMapping("/reset-password")
    public OkResponse reset(@Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(req.token(), req.newPassword());
        return new OkResponse(true);
    }
}
