package com.example.Ecomm.controller;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.service.InvoiceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping(value = "/generate/{orderId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') " +
                    "or hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')"
    )
    public ResponseEntity<byte[]> generateInvoice(@PathVariable Long orderId) {
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData(
                    "attachment",
                    "invoice-" + orderId + ".pdf"
            );
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error(
                    "Error generating PDF for orderId={}",
                    orderId,
                    e
            );
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (RuntimeException e) {
            logger.error(
                    "Order not found or invalid for orderId={}",
                    orderId,
                    e
            );
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
