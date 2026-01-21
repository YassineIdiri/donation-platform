package com.yassine.donationplatform.dto.response;

import java.util.UUID;

public class CheckoutSessionResponse {
    private UUID donationId;
    private String checkoutUrl;

    public CheckoutSessionResponse(UUID donationId, String checkoutUrl) {
        this.donationId = donationId;
        this.checkoutUrl = checkoutUrl;
    }

    public UUID getDonationId() { return donationId; }
    public String getCheckoutUrl() { return checkoutUrl; }
}