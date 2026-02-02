package com.example.Ecomm.controller;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.service.InvoiceService;
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

	private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    
    @GetMapping(value = "/generate/{orderId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
    public ResponseEntity<byte[]> generateInvoice(@PathVariable Long orderId) {
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "invoice-" + orderId + ".pdf");
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            System.err.println("Error generating PDF for order ID " + orderId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            System.err.println("Order not found for ID " + orderId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}

