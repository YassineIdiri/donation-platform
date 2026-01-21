package com.yassine.donationplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdateSettingsRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String primaryColor; // ex "#0ea5e9"

    @NotNull
    @Size(min = 1, max = 8)
    private List<Integer> suggestedAmounts;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public List<Integer> getSuggestedAmounts() { return suggestedAmounts; }
    public void setSuggestedAmounts(List<Integer> suggestedAmounts) { this.suggestedAmounts = suggestedAmounts; }
}
