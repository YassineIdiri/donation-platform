// src/main/java/com/yassine/donationplatform/service/SettingsService.java
package com.yassine.donationplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yassine.donationplatform.domain.settings.AppSettings;
import com.yassine.donationplatform.dto.request.UpdateSettingsRequest;
import com.yassine.donationplatform.dto.response.SettingsResponse;
import com.yassine.donationplatform.repository.AppSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SettingsService {

    private static final String KEY_PUBLIC_UI = "PUBLIC_UI";

    private final AppSettingsRepository repo;
    private final ObjectMapper objectMapper;

    public SettingsService(AppSettingsRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public SettingsResponse getPublicUi() {
        AppSettings row = repo.findBySettingsKey(KEY_PUBLIC_UI).orElse(null);
        if (row == null) {
            return defaultResponse(); // ✅ pas de 500 au premier boot
        }

        String jsonStr = row.getSettingsJson();
        if (jsonStr == null || jsonStr.isBlank()) {
            return defaultResponse();
        }

        try {
            JsonNode root = objectMapper.readTree(jsonStr);

            String title = root.path("title").asText("Association Solidaire");
            String color = root.path("primaryColor").asText("#0ea5e9");

            List<Integer> amounts = new ArrayList<>();
            JsonNode arr = root.path("suggestedAmounts");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    if (n != null && n.isInt()) {
                        int v = n.asInt();
                        if (v > 0) amounts.add(v);
                    } else if (n != null && n.isTextual()) {
                        try {
                            int v = Integer.parseInt(n.asText());
                            if (v > 0) amounts.add(v);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (amounts.isEmpty()) amounts = List.of(10, 20, 50, 100);
            else amounts = amounts.stream().distinct().sorted().limit(8).toList();

            return new SettingsResponse(title, color, amounts);
        } catch (Exception e) {
            return defaultResponse();
        }
    }

    @Transactional
    public SettingsResponse updatePublicUi(UpdateSettingsRequest req) {
        List<Integer> cleaned = req.getSuggestedAmounts().stream()
                .filter(a -> a != null && a > 0 && a <= 100000)
                .distinct()
                .sorted()
                .limit(8)
                .toList();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("suggestedAmounts must contain positive integers");
        }

        try {
            JsonNode json = objectMapper.createObjectNode()
                    .put("title", req.getTitle())
                    .put("primaryColor", req.getPrimaryColor())
                    .set("suggestedAmounts", objectMapper.valueToTree(cleaned));

            String jsonStr = objectMapper.writeValueAsString(json);

            AppSettings row = repo.findBySettingsKey(KEY_PUBLIC_UI)
                    .orElseGet(() -> {
                        AppSettings s = new AppSettings();
                        s.setSettingsKey(KEY_PUBLIC_UI);
                        s.setSettingsJson("{}");
                        return s;
                    });

            row.setSettingsJson(jsonStr);
            repo.save(row);

            return new SettingsResponse(req.getTitle(), req.getPrimaryColor(), cleaned);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update settings", e);
        }
    }

    private SettingsResponse defaultResponse() {
        return new SettingsResponse("Association Solidaire", "#0ea5e9", List.of(10, 20, 50, 100));
    }
}
