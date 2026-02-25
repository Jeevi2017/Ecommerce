package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.OrderItemDTO;
import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.entitiy.OrderItem;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.repository.OrderItemRepository;
import com.example.Ecomm.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    // ---------- HELPERS ----------

    private Product mockProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockQuantity(10L);
        return product;
    }

    private OrderItem mockOrderItem(Product product) {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(2L);
        orderItem.setPrice(product.getPrice());
        orderItem.setProduct(product);
        return orderItem;
    }

    private OrderItemDTO mockOrderItemDTO() {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(1L);

        OrderItemDTO dto = new OrderItemDTO();
        dto.setQuantity(2L);
        dto.setProductDetails(productDTO);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void saveOrderItem() {
        Product product = mockProduct();
        OrderItem orderItem = mockOrderItem(product);
        OrderItemDTO dto = mockOrderItemDTO();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);

        OrderItemDTO result = orderItemService.saveOrderItem(dto);

        assertNotNull(result);
        assertEquals(2L, result.getQuantity());
    }

    @Test
    void getAllOrderItems() {
        Product product = mockProduct();
        OrderItem item1 = mockOrderItem(product);
        OrderItem item2 = mockOrderItem(product);

        when(orderItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<OrderItemDTO> result = orderItemService.getAllOrderItems();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getOrderItemById() {
        Product product = mockProduct();
        OrderItem orderItem = mockOrderItem(product);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        OrderItemDTO result = orderItemService.getOrderItemById(1L);

        assertNotNull(result);
        assertEquals(2L, result.getQuantity());
    }
}
