package com.example.Ecomm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.service.IQRCodeService;
import com.example.Ecomm.service.OrderService;
import com.example.Ecomm.dto.OrderDTO;

@RestController
@RequestMapping("/api/qrcode")
@CrossOrigin(origins = "http://localhost:4200")
public class QRCodeController {

    private final IQRCodeService qrCodeService;
    private final OrderService orderService;

    @Value("${razorpay.vpa}")
    private String razorpayVpa;

    @Autowired
    public QRCodeController(IQRCodeService qrCodeService,
                            OrderService orderService) {
        this.qrCodeService = qrCodeService;
        this.orderService = orderService;
    }

    // ================= GENERIC QR =================

    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQRCode(@RequestParam String data) {
        try {
            byte[] qrCodeImage = qrCodeService.generateQRCodeImage(data, 200, 200);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ================= ORDER BASED UPI QR (NO RAZORPAY) =================

    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
    @GetMapping(value = "/order-payment/{orderId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateOrderPaymentQR(@PathVariable Long orderId) {
        try {

            // 1️⃣ Check VPA configured
            if (razorpayVpa == null || razorpayVpa.trim().isEmpty()) {
                throw new IllegalArgumentException("UPI VPA not configured in application.properties");
            }

            // 2️⃣ Get order from DB
            OrderDTO order = orderService.getOrderById(orderId);

            if (order == null) {
                throw new IllegalArgumentException("Order not found");
            }

            if (order.getTotalAmount() == null || order.getTotalAmount().doubleValue() <= 0) {
                throw new IllegalArgumentException("Invalid order amount");
            }

            // 3️⃣ Build UPI payment URI
            String qrData = UriComponentsBuilder.fromUriString("upi://pay")
                    .queryParam("pa", razorpayVpa)
                    .queryParam("pn", "E-Commerce App")
                    .queryParam("am", String.format("%.2f", order.getTotalAmount()))
                    .queryParam("cu", "INR")
                    .queryParam("tn", "Order-" + orderId)
                    .build()
                    .toUriString();

            System.out.println("Generated UPI QR Data: " + qrData);

            // 4️⃣ Generate QR image
            byte[] qrCodeImage = qrCodeService.generateQRCodeImage(qrData, 250, 250);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
