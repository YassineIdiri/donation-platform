// src/main/java/com/yassine/donationplatform/web/admin/SettingsAdminController.java
package com.yassine.donationplatform.web.admin;

import com.yassine.donationplatform.dto.request.UpdateSettingsRequest;
import com.yassine.donationplatform.dto.response.SettingsResponse;
import com.yassine.donationplatform.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
public class SettingsAdminController {

    private final SettingsService settingsService;

    public SettingsAdminController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    // ✅ Front appelle GET /api/admin/settings/public-ui
    @GetMapping("/public-ui")
    public SettingsResponse getPublicUi() {
        return settingsService.getPublicUi();
    }

    // ✅ Front appelle PUT /api/admin/settings/public-ui
    @PutMapping("/public-ui")
    public SettingsResponse updatePublicUi(@Valid @RequestBody UpdateSettingsRequest req) {
        return settingsService.updatePublicUi(req);
    }
}
