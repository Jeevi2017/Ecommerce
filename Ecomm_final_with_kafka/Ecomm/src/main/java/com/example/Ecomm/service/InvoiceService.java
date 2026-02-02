package com.example.Ecomm.service;

import java.io.IOException;

public interface InvoiceService {

	byte[] generateInvoicePdf(Long orderId) throws IOException;
}
