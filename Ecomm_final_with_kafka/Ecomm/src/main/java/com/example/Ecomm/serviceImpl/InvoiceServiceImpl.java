package com.example.Ecomm.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.example.Ecomm.dto.InvoiceDto;
import com.example.Ecomm.dto.InvoiceItemDto;
import com.example.Ecomm.entitiy.Order;
import com.example.Ecomm.entitiy.OrderItem;
import com.example.Ecomm.repository.OrderRepository;
import com.example.Ecomm.service.InvoiceService;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    // ✅ Constructor Injection (SonarQube compliant)
    public InvoiceServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private static final PDType1Font FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font FONT_NORMAL = PDType1Font.HELVETICA;
    private static final float MARGIN = 50;
    private static final float HEADER_HEIGHT = 70;
    private static final float FOOTER_HEIGHT = 40;
    private static final float TABLE_ROW_HEIGHT = 20;

    @Override
    public byte[] generateInvoicePdf(Long orderId) throws IOException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        InvoiceDto invoiceDto = mapOrderToInvoiceDto(order);
        return createPdfFromDto(invoiceDto);
    }

    private InvoiceDto mapOrderToInvoiceDto(Order order) {
        InvoiceDto invoiceDto = new InvoiceDto();
        invoiceDto.setOrderId(order.getId());
        invoiceDto.setCustomerName(order.getCustomer().getUsername());
        invoiceDto.setCustomerEmail(order.getCustomer().getEmail());
        invoiceDto.setOrderDate(order.getOrderDate());

        BigDecimal subtotal = order.getTotalAmount().add(order.getDiscountAmount());
        invoiceDto.setSubtotal(subtotal);
        invoiceDto.setDiscount(order.getDiscountAmount());
        invoiceDto.setTotal(order.getTotalAmount());

        List<InvoiceItemDto> items = order.getOrderItems()
                .stream()
                .map(this::mapOrderItemToDto)
                .toList();

        invoiceDto.setItems(items);
        return invoiceDto;
    }

    private InvoiceItemDto mapOrderItemToDto(OrderItem item) {
        InvoiceItemDto dto = new InvoiceItemDto();
        dto.setProductName(item.getProduct().getName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getPrice());
        dto.setItemTotal(item.getItemTotal());
        return dto;
    }

    private byte[] createPdfFromDto(InvoiceDto invoiceDto) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);

            float yPosition = currentPage.getMediaBox().getHeight() - MARGIN - HEADER_HEIGHT;
            int pageNumber = 1;

            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);
            drawHeaderAndFooter(contentStream, currentPage, pageNumber, invoiceDto);

            // ---- Invoice Details ----
            contentStream.beginText();
            contentStream.setFont(FONT_NORMAL, 12);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Invoice ID: " + invoiceDto.getOrderId());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Date: " + invoiceDto.getOrderDate()
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Customer Name: " + invoiceDto.getCustomerName());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Customer Email: " + invoiceDto.getCustomerEmail());
            contentStream.endText();

            yPosition -= 75;

            // ---- Table Header ----
            contentStream.setFont(FONT_BOLD, 12);
            float[] columnWidths = {200, 70, 80, 80};
            String[] headers = {"Product", "Qty", "Unit Price", "Total"};
            float x = MARGIN;

            contentStream.beginText();
            contentStream.newLineAtOffset(x, yPosition);
            for (int i = 0; i < headers.length; i++) {
                contentStream.showText(headers[i]);
                contentStream.newLineAtOffset(columnWidths[i], 0);
            }
            contentStream.endText();

            yPosition -= 10;

            // ---- Items ----
            contentStream.setFont(FONT_NORMAL, 10);
            for (InvoiceItemDto item : invoiceDto.getItems()) {
                x = MARGIN;
                contentStream.beginText();
                contentStream.newLineAtOffset(x, yPosition);
                contentStream.showText(item.getProductName());
                contentStream.newLineAtOffset(columnWidths[0], 0);
                contentStream.showText(String.valueOf(item.getQuantity()));
                contentStream.newLineAtOffset(columnWidths[1], 0);
                contentStream.showText(item.getUnitPrice().toString());
                contentStream.newLineAtOffset(columnWidths[2], 0);
                contentStream.showText(item.getItemTotal().toString());
                contentStream.endText();
                yPosition -= TABLE_ROW_HEIGHT;
            }

            // ---- Totals ----
            yPosition -= 20;
            contentStream.beginText();
            contentStream.setFont(FONT_BOLD, 12);
            float totalX = MARGIN + columnWidths[0] + columnWidths[1] + 20;
            contentStream.newLineAtOffset(totalX, yPosition);
            contentStream.showText("Subtotal: " + invoiceDto.getSubtotal());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Discount: " + invoiceDto.getDiscount());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Total: " + invoiceDto.getTotal());
            contentStream.endText();

            contentStream.close();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void drawHeaderAndFooter(
            PDPageContentStream contentStream,
            PDPage page,
            int pageNumber,
            InvoiceDto invoiceDto) throws IOException {

        PDRectangle mediaBox = page.getMediaBox();
        float width = mediaBox.getWidth();
        float height = mediaBox.getHeight();

        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 24);
        contentStream.newLineAtOffset(MARGIN, height - MARGIN);
        contentStream.showText("INVOICE");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(FONT_NORMAL, 10);
        float companyInfoX = width - MARGIN - 150;
        contentStream.newLineAtOffset(companyInfoX, height - MARGIN - 5);
        contentStream.showText("Ecomm Pvt. Ltd.");
        contentStream.newLineAtOffset(0, -15);
        contentStream.showText("123, E-commerce Avenue");
        contentStream.newLineAtOffset(0, -15);
        contentStream.showText("Invoice Date: " +
                invoiceDto.getOrderDate()
                        .toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(FONT_NORMAL, 10);
        float pageTextWidth = FONT_NORMAL.getStringWidth("Page " + pageNumber) / 1000 * 10;
        float pageX = (width - pageTextWidth) / 2;
        contentStream.newLineAtOffset(pageX, MARGIN - 20);
        contentStream.showText("Page " + pageNumber);
        contentStream.endText();
    }
}
