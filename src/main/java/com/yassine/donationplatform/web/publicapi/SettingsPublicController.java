package com.yassine.donationplatform.web.publicapi;

import com.yassine.donationplatform.dto.response.SettingsResponse;
import com.yassine.donationplatform.service.SettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SettingsPublicController {

    private final SettingsService settingsService;

    public SettingsPublicController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/settings")
    public SettingsResponse getSettings() {
        return settingsService.getPublicUi();
    }
}
