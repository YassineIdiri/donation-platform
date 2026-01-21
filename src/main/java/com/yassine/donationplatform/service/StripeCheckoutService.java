package com.yassine.donationplatform.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.yassine.donationplatform.domain.donation.Donation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeCheckoutService {

    @Value("${app.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.stripe.success-url}")
    private String successUrlTemplate;

    @Value("${app.stripe.cancel-url}")
    private String cancelUrlTemplate;

    public Session createCheckoutSession(Donation donation) throws Exception {
        Stripe.apiKey = stripeSecretKey;

        String successUrl = successUrlTemplate.replace("{DONATION_ID}", donation.getId().toString());
        String cancelUrl  = cancelUrlTemplate.replace("{DONATION_ID}", donation.getId().toString());

        var productData = SessionCreateParams.LineItem.PriceData.ProductData.builder()
                .setName("Don à l'association")
                .build();

        var priceData = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(donation.getCurrency().toLowerCase())
                .setUnitAmount((long) donation.getAmountCents())
                .setProductData(productData)
                .build();

        var lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(priceData)
                .build();

        var builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(lineItem)
                .putMetadata("donationId", donation.getId().toString());

        if (donation.getEmail() != null && !donation.getEmail().isBlank()) {
            builder.setCustomerEmail(donation.getEmail());
        }

        return Session.create(builder.build());
    }
}
