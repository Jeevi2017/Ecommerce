package com.example.Ecomm.controller;

import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.dto.OrderItemDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.service.OrderItemService;
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
class OrderItemControllerTest {

    @InjectMocks
    private OrderItemController orderItemController;

    @Mock
    private OrderItemService orderItemService;

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

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername("testuser");

        // ✅ lenient to avoid UnnecessaryStubbing
        lenient()
                .when(userService.getUserByUserName("testuser"))
                .thenReturn(userDTO);
    }

    private OrderItemDTO mockOrderItemDTO() {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(1L);
        return dto;
    }

    private OrderDTO mockOrderDTO() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setCustomerId(1L);
        return orderDTO;
    }

    // ---------- TESTS ----------

    @Test
    void getAuthenticatedCustomerId() {
        mockAuthenticatedUser(1L);

        Long customerId = orderItemController.getAuthenticatedCustomerId();

        assertEquals(1L, customerId);
    }

    @Test
    void getOrderItemOwnerId() {
        when(orderItemService.getOrderItemById(1L))
                .thenReturn(mockOrderItemDTO());

        when(orderService.getOrderById(1L))
                .thenReturn(mockOrderDTO());

        Long ownerId = orderItemController.getOrderItemOwnerId(1L);

        assertEquals(1L, ownerId);
    }

    @Test
    void createOrderItem() {
        when(orderItemService.saveOrderItem(any()))
                .thenReturn(mockOrderItemDTO());

        ResponseEntity<OrderItemDTO> response =
                orderItemController.createOrderItem(mockOrderItemDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getAllOrderItems() {
        when(orderItemService.getAllOrderItems())
                .thenReturn(List.of(mockOrderItemDTO()));

        ResponseEntity<List<OrderItemDTO>> response =
                orderItemController.getAllOrderItems();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getOrderItemById() {
        mockAuthenticatedUser(1L);

        when(orderItemService.getOrderItemById(1L))
                .thenReturn(mockOrderItemDTO());

        ResponseEntity<OrderItemDTO> response =
                orderItemController.getOrderItemById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
