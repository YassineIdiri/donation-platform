package com.yassine.donationplatform.service.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.yassine.donationplatform.entity.donation.Donation;
import com.yassine.donationplatform.dto.DonationStatus;
import com.yassine.donationplatform.service.donation.DonationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final DonationService donationService;
    private final ObjectMapper objectMapper;

    public StripeWebhookService(DonationService donationService, ObjectMapper objectMapper) {
        this.donationService = donationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handle(Event event) {
        log.info("Stripe webhook received type={}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            handleCheckoutCompleted(event);
        }
    }

    private void handleCheckoutCompleted(Event event) {
        try {
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            JsonNode root = objectMapper.readTree(rawJson);

            String sessionId = textOrNull(root, "id");
            String paymentStatus = textOrNull(root, "payment_status");
            String paymentIntent = textOrNull(root, "payment_intent");
            String donationIdStr = root.path("metadata").path("donationId").asText(null);

            if (donationIdStr == null || donationIdStr.isBlank()) {
                return;
            }

            Donation donation = donationService.findById(UUID.fromString(donationIdStr)).orElse(null);
            if (donation == null) {
                return;
            }

            if (donation.getStripeCheckoutSessionId() == null && sessionId != null) {
                donation.setStripeCheckoutSessionId(sessionId);
            }

            if ("paid".equalsIgnoreCase(paymentStatus)) {
                donation.setStatus(DonationStatus.PAID);
                donation.setStripePaymentIntentId(paymentIntent);
            } else {
                donation.setStatus(DonationStatus.FAILED);
            }

            donationService.save(donation);

        } catch (Exception e) {
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
