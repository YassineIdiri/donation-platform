package com.yassine.donationplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.yassine.donationplatform.domain.donation.Donation;
import com.yassine.donationplatform.domain.donation.DonationStatus;
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
        } else {
            // MVP: on ignore le reste
            log.info("Stripe webhook ignored type={}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        try {
            // 1) On récupère l'objet JSON brut (marche même si la désérialisation en Session échoue)
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            JsonNode root = objectMapper.readTree(rawJson);

            String sessionId = textOrNull(root, "id");
            String paymentStatus = textOrNull(root, "payment_status");
            String paymentIntent = textOrNull(root, "payment_intent");
            String donationIdStr = root.path("metadata").path("donationId").asText(null);

            log.info("checkout.session.completed sessionId={} paymentStatus={} donationId={}",
                    sessionId, paymentStatus, donationIdStr);

            if (donationIdStr == null || donationIdStr.isBlank()) {
                log.warn("Missing metadata.donationId on checkout.session.completed (sessionId={})", sessionId);
                return;
            }

            Donation donation = donationService.findById(UUID.fromString(donationIdStr)).orElse(null);
            if (donation == null) {
                log.warn("Donation not found for donationId={} (sessionId={})", donationIdStr, sessionId);
                return;
            }

            // (optionnel) garde trace de sessionId si pas déjà stocké
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
            log.info("Donation updated id={} newStatus={}", donation.getId(), donation.getStatus());

        } catch (Exception e) {
            // On log l'erreur pour comprendre (sinon tu restes dans le flou)
            log.error("Failed to handle checkout.session.completed", e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
