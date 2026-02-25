package com.example.Ecomm.controller;

import com.example.Ecomm.service.PaymentService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @InjectMocks
    private PaymentWebhookController paymentWebhookController;

    @Mock
    private PaymentService paymentService;

    @Test
    void handleRazorpayWebhook_success() throws Exception {
        // ---------- GIVEN ----------
        String payload = new JSONObject()
                .put("event", "payment.captured")
                .put("payload", new JSONObject()
                        .put("payment", new JSONObject()
                                .put("entity", new JSONObject()
                                        .put("order_id", "order_xyz")
                                        .put("notes", new JSONObject()
                                                .put("internal_order_id", "1")
                                        )
                                )
                        )
                ).toString();

        String signature = "valid-signature";

        ReflectionTestUtils.setField(
                paymentWebhookController,
                "webhookSecret",
                "test-secret"
        );

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {

            mockedUtils
                    .when(() -> Utils.verifyWebhookSignature(
                            payload, signature, "test-secret"))
                    .thenReturn(true);

            // ---------- WHEN ----------
            ResponseEntity<String> response =
                    paymentWebhookController.handleRazorpayWebhook(payload, signature);

            // ---------- THEN ----------
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(paymentService)
                    .captureWebhookPayment(any(JSONObject.class), eq(1L));
        }
    }

    @Test
    void handleRazorpayWebhook_invalidSignature() throws Exception {
        String payload = "{}";
        String signature = "invalid";

        ReflectionTestUtils.setField(
                paymentWebhookController,
                "webhookSecret",
                "test-secret"
        );

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {

            mockedUtils
                    .when(() -> Utils.verifyWebhookSignature(
                            payload, signature, "test-secret"))
                    .thenReturn(false);

            ResponseEntity<String> response =
                    paymentWebhookController.handleRazorpayWebhook(payload, signature);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
