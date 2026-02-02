package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Order;
import com.example.Ecomm.entitiy.OrderItem;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class InvoiceServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Order dummyOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Dummy Customer
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setUsername("John Doe");
        customer.setEmail("john@example.com");

        // Dummy Product
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");

        // Dummy OrderItem
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2L);                  // use Long instead of int
        item.setPrice(BigDecimal.valueOf(50)); // unit price
        // No setItemTotal() - remove it

        // Add items to list
        List<OrderItem> items = new ArrayList<>();
        items.add(item);

        // Dummy Order
        dummyOrder = new Order();
        dummyOrder.setId(1L);
        dummyOrder.setCustomer(customer);
        dummyOrder.setOrderDate(LocalDateTime.now());
        dummyOrder.setOrderItems(items);
        dummyOrder.setTotalAmount(BigDecimal.valueOf(100));   // total amount
        dummyOrder.setDiscountAmount(BigDecimal.valueOf(0));  // discount

        // Mock repository call
        when(orderRepository.findById(1L)).thenReturn(Optional.of(dummyOrder));
    }

    @Test
    void testGenerateInvoicePdf() throws Exception {
        // Call method
        byte[] pdfBytes = invoiceService.generateInvoicePdf(1L);

        // Ensure it returns non-null bytes
        assertNotNull(pdfBytes);
        // Ensure PDF has some content
        assert(pdfBytes.length > 0);
    }
}
