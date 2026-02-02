package com.example.Ecomm.controller;

import com.example.Ecomm.service.PaymentService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/razorpay-webhook")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        logger.info("Received Razorpay webhook. Signature: {}", signature);
        logger.debug("Webhook Payload: {}", payload);

        try {
            // Verify the webhook signature to ensure it's from Razorpay
            boolean isSignatureValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isSignatureValid) {
                logger.warn("Invalid webhook signature. Request will be ignored.");
                return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
            }

            JSONObject webhookPayload = new JSONObject(payload);
            String event = webhookPayload.getString("event");

            logger.info("Processing Razorpay event: {}", event);

            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = webhookPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                
                // For Razorpay Orders API, the internal order ID is often stored in notes
                JSONObject notes = paymentEntity.getJSONObject("notes");
                Long internalOrderId = Long.parseLong(notes.getString("internal_order_id"));

                // Call the service to update the order status
                paymentService.captureWebhookPayment(paymentEntity, internalOrderId);

                logger.info("Successfully processed payment for Order ID: {}", internalOrderId);
            }

            return new ResponseEntity<>("Webhook received successfully", HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error processing Razorpay webhook:", e);
            return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}