package com.example.Ecomm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder; // <-- Add this import

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.service.IQRCodeService;
import com.example.Ecomm.service.PaymentService;
import com.example.Ecomm.dto.RazorpayOrderRequestDTO;
import com.example.Ecomm.dto.RazorpayOrderResponseDTO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/qrcode")
@CrossOrigin(origins = "http://localhost:4200")
public class QRCodeController {

	private final IQRCodeService qrCodeService;
    private final PaymentService paymentService;

    @Value("${razorpay.vpa}")
    private String razorpayVpa;

    @Autowired
    public QRCodeController(IQRCodeService qrCodeService, PaymentService paymentService) {
        this.qrCodeService = qrCodeService;
        this.paymentService = paymentService;
    }

    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQRCode(@RequestParam String data) {
        try {
            int width = 200;
            int height = 200;
            byte[] qrCodeImage = qrCodeService.generateQRCodeImage(data, width, height);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
    @PostMapping(value = "/generateForPayment", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generatePaymentQRCode(@RequestBody RazorpayOrderRequestDTO orderRequestDTO) {
        try {
            if (razorpayVpa == null || razorpayVpa.trim().isEmpty()) {
                throw new IllegalArgumentException("Razorpay VPA is not configured. Cannot generate QR code.");
            }

            RazorpayOrderResponseDTO razorpayOrder = paymentService.createRazorpayOrder(orderRequestDTO);

            // Construct the UPI URI using UriComponentsBuilder for proper encoding
            String qrCodeData = UriComponentsBuilder.fromUriString("upi://pay")
                .queryParam("pa", razorpayVpa)
                .queryParam("pn", "E-commerce Payment")
                .queryParam("am", String.format("%.2f", razorpayOrder.getAmount().doubleValue() / 100))
                .queryParam("cu", "INR")
                .queryParam("tr", razorpayOrder.getId())
                .build()
                .toUriString();

            System.out.println("Generated UPI QR Data: " + qrCodeData);

            int width = 250;
            int height = 250;
            byte[] qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeData, width, height);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (IllegalArgumentException e) {
            System.err.println("QR Code Generation Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
    @PostMapping(value = "/generateRazorpayPageQRCode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateRazorpayPageQRCode(@RequestBody RazorpayOrderRequestDTO requestDTO) {
        try {
            RazorpayOrderResponseDTO razorpayOrder = paymentService.createRazorpayOrder(requestDTO);

            String paymentPageUrl = "https://rzp.io/i/" + razorpayOrder.getId();
            
            
            System.out.println("Final QR Code URL to be encoded: " + paymentPageUrl);


            byte[] qrCodeBytes = qrCodeService.generateQRCodeFromUrl(paymentPageUrl, 250, 250);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}