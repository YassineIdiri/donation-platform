package com.yassine.donationplatform.dto.response;

import java.util.List;

public class SettingsResponse {
    private String title;
    private String primaryColor;
    private List<Integer> suggestedAmounts;

    public SettingsResponse(String title, String primaryColor, List<Integer> suggestedAmounts) {
        this.title = title;
        this.primaryColor = primaryColor;
        this.suggestedAmounts = suggestedAmounts;
    }

    public String getTitle() { return title; }
    public String getPrimaryColor() { return primaryColor; }
    public List<Integer> getSuggestedAmounts() { return suggestedAmounts; }
}
