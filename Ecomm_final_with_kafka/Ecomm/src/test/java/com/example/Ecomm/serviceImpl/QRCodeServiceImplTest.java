package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.service.IQRCodeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeServiceImplTest {

    private final IQRCodeService qrCodeService = new QRCodeServiceImpl();

    @Test
    void generateQRCodeImage() throws Exception {
        // GIVEN
        String data = "Test QR Code Data";
        int width = 200;
        int height = 200;

        // WHEN
        byte[] qrCode = qrCodeService.generateQRCodeImage(data, width, height);

        // THEN
        assertNotNull(qrCode);
        assertTrue(qrCode.length > 0, "QR code byte array should not be empty");
    }

    @Test
    void generateQRCodeFromUrl() throws Exception {
        // GIVEN
        String url = "https://example.com";
        int width = 200;
        int height = 200;

        // WHEN
        byte[] qrCode = qrCodeService.generateQRCodeFromUrl(url, width, height);

        // THEN
        assertNotNull(qrCode);
        assertTrue(qrCode.length > 0, "QR code byte array should not be empty");
    }
}
