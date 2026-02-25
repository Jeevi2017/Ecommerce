package com.example.Ecomm.controller;

import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.service.OrderService;
import com.example.Ecomm.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @InjectMocks
    private OrderController orderController;

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- HELPERS ----------

    private void mockAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );

        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setUsername("testuser");

        // ✅ FIX: lenient stubbing
        lenient()
                .when(userService.getUserByUserName("testuser"))
                .thenReturn(user);
    }

    private OrderDTO mockOrderDTO() {
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);
        dto.setCustomerId(1L);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getAuthenticatedCustomerId() {
        mockAuthenticatedUser(1L);

        Long customerId = orderController.getAuthenticatedCustomerId();

        assertEquals(1L, customerId);
    }

    @Test
    void getCustomerIdByOrderId() {
        when(orderService.getOrderById(1L))
                .thenReturn(mockOrderDTO());

        Long customerId =
                orderController.getCustomerIdByOrderId(1L);

        assertEquals(1L, customerId);
    }

    @Test
    void getAllOrders() {
        when(orderService.getAllOrders())
                .thenReturn(List.of(mockOrderDTO()));

        ResponseEntity<List<OrderDTO>> response =
                orderController.getAllOrders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deleteOrder() {
        doNothing().when(orderService).deleteOrder(1L);

        ResponseEntity<Void> response =
                orderController.deleteOrder(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void updateOrder() {
        when(orderService.updateOrderFull(eq(1L), any()))
                .thenReturn(mockOrderDTO());

        ResponseEntity<OrderDTO> response =
                orderController.updateOrder(1L, mockOrderDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getOrderById() {
        mockAuthenticatedUser(1L);

        when(orderService.getOrderById(1L))
                .thenReturn(mockOrderDTO());

        ResponseEntity<OrderDTO> response =
                orderController.getOrderById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getOrdersByCustomerId() {
        mockAuthenticatedUser(1L);

        when(orderService.getOrdersByCustomerId(1L))
                .thenReturn(List.of(mockOrderDTO()));

        ResponseEntity<List<OrderDTO>> response =
                orderController.getOrdersByCustomerId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createOrderFromCart() {
        mockAuthenticatedUser(1L);

        when(orderService.placeOrder(1L, 10L))
                .thenReturn(mockOrderDTO());

        ResponseEntity<OrderDTO> response =
                orderController.createOrderFromCart(1L, 10L);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
