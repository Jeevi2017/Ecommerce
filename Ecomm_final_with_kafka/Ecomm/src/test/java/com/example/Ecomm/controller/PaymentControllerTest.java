package com.example.Ecomm.controller;

import com.example.Ecomm.dto.*;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.OrderService;
import com.example.Ecomm.service.PaymentService;
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
class PaymentControllerTest {

    @InjectMocks
    private PaymentController paymentController;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CustomerService customerService;

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

        lenient()
                .when(userService.getUserByUserName("testuser"))
                .thenReturn(userDTO);
    }

    private PaymentDTO mockPaymentDTO() {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(1L);
        dto.setOrderId(10L);
        return dto;
    }

    private OrderDTO mockOrderDTO() {
        OrderDTO dto = new OrderDTO();
        dto.setId(10L);
        dto.setCustomerId(1L);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getAuthenticatedCustomerId() {
        mockAuthenticatedUser(1L);

        Long id = paymentController.getAuthenticatedCustomerId();

        assertEquals(1L, id);
    }

    @Test
    void getOrderOwnerId() {
        when(orderService.getOrderById(10L))
                .thenReturn(mockOrderDTO());

        Long ownerId = paymentController.getOrderOwnerId(10L);

        assertEquals(1L, ownerId);
    }

    @Test
    void getPaymentOwnerId() {
        when(paymentService.getPaymentById(1L))
                .thenReturn(mockPaymentDTO());

        when(orderService.getOrderById(10L))
                .thenReturn(mockOrderDTO());

        Long ownerId = paymentController.getPaymentOwnerId(1L);

        assertEquals(1L, ownerId);
    }

    @Test
    void processPayment() {
        when(paymentService.processPayment(eq(10L), any()))
                .thenReturn(mockPaymentDTO());

        ResponseEntity<PaymentDTO> response =
                paymentController.processPayment(10L, mockPaymentDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void createRazorpayOrder() throws Exception {
        RazorpayOrderRequestDTO request = new RazorpayOrderRequestDTO();
        RazorpayOrderResponseDTO responseDTO = new RazorpayOrderResponseDTO();

        when(paymentService.createRazorpayOrder(any()))
                .thenReturn(responseDTO);

        ResponseEntity<RazorpayOrderResponseDTO> response =
                paymentController.createRazorpayOrder(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void captureRazorpayPayment() throws Exception {
        RazorpayPaymentCaptureRequestDTO request =
                new RazorpayPaymentCaptureRequestDTO();

        when(paymentService.captureRazorpayPayment(any()))
                .thenReturn(mockPaymentDTO());

        ResponseEntity<PaymentDTO> response =
                paymentController.captureRazorpayPayment(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getPaymentById() {
        when(paymentService.getPaymentById(1L))
                .thenReturn(mockPaymentDTO());

        ResponseEntity<PaymentDTO> response =
                paymentController.getPaymentById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void getAllPayments() {
        when(paymentService.getAllPayments())
                .thenReturn(List.of(mockPaymentDTO()));

        ResponseEntity<List<PaymentDTO>> response =
                paymentController.getAllPayments();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPaymentsByOrderId() {
        when(paymentService.getPaymentsByOrderId(10L))
                .thenReturn(List.of(mockPaymentDTO()));

        ResponseEntity<List<PaymentDTO>> response =
                paymentController.getPaymentsByOrderId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deletePayment() {
        doNothing().when(paymentService).deletePayment(1L);

        ResponseEntity<Void> response =
                paymentController.deletePayment(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
