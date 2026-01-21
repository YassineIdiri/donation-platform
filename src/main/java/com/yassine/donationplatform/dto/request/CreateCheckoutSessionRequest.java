package com.yassine.donationplatform.dto.request;

import jakarta.validation.constraints.*;

public class CreateCheckoutSessionRequest {

    @Min(1)
    private int amount; // en euros côté front (ex: 20)

    @Email
    private String email; // optionnel

    // "CARD" ou "PAYPAL" (PAYPAL peut être ignoré en MVP)
    @NotBlank
    private String paymentMethod;

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
