package com.yassine.donationplatform.dto.response;

import java.util.List;

public class SettingsResponse {
    private String title;
    private List<Integer> suggestedAmounts;

    public SettingsResponse(String title, List<Integer> suggestedAmounts) {
        this.title = title;
        this.suggestedAmounts = suggestedAmounts;
    }

    public String getTitle() { return title; }
    public List<Integer> getSuggestedAmounts() { return suggestedAmounts; }
}
