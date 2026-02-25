package com.example.Ecomm.controller;

import com.example.Ecomm.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @InjectMocks
    private InvoiceController invoiceController;

    @Mock
    private InvoiceService invoiceService;

    @Test
    void generateInvoice_success() throws Exception {
        byte[] pdfBytes = "dummy-pdf".getBytes();

        when(invoiceService.generateInvoicePdf(1L))
                .thenReturn(pdfBytes);

        ResponseEntity<byte[]> response =
                invoiceController.generateInvoice(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(pdfBytes, response.getBody());
        assertTrue(
                response.getHeaders().getContentDisposition()
                        .getFilename()
                        .contains("invoice-1")
        );
    }

    @Test
    void generateInvoice_ioException() throws Exception {
        when(invoiceService.generateInvoicePdf(1L))
                .thenThrow(new IOException("PDF error"));

        ResponseEntity<byte[]> response =
                invoiceController.generateInvoice(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void generateInvoice_runtimeException() throws Exception {
        when(invoiceService.generateInvoicePdf(1L))
                .thenThrow(new RuntimeException("Order not found"));

        ResponseEntity<byte[]> response =
                invoiceController.generateInvoice(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
