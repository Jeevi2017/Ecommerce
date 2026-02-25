package com.example.Ecomm.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.dto.PaymentDTO;
import com.example.Ecomm.dto.RazorpayOrderRequestDTO;
import com.example.Ecomm.dto.RazorpayOrderResponseDTO;
import com.example.Ecomm.dto.RazorpayPaymentCaptureRequestDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.OrderService;
import com.example.Ecomm.service.PaymentService;
import com.example.Ecomm.service.UserService;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;

    public PaymentController(
            PaymentService paymentService,
            OrderService orderService,
            UserService userService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.userService = userService;
    }

    // ================= AUTH HELPERS =================

    public Long getAuthenticatedCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        UserDTO userDTO = userService.getUserByUserName(authenticatedUsername);
        return userDTO.getId();
    }

    public Long getOrderOwnerId(Long orderId) {
        OrderDTO orderDTO = orderService.getOrderById(orderId);
        if (orderDTO != null) {
            return orderDTO.getCustomerId();
        }
        throw new ResourceNotFoundException("Order", "Id for owner check", orderId);
    }

    public Long getPaymentOwnerId(Long paymentId) {
        PaymentDTO paymentDTO = paymentService.getPaymentById(paymentId);
        if (paymentDTO != null && paymentDTO.getOrderId() != null) {
            return getOrderOwnerId(paymentDTO.getOrderId());
        }
        throw new ResourceNotFoundException("Payment or its associated Order", "Id for owner check", paymentId);
    }

    // ================= APIs =================

    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')")
    public ResponseEntity<PaymentDTO> processPayment(
            @PathVariable Long orderId,
            @Validated @RequestBody PaymentDTO paymentDTO) {

        PaymentDTO savedPayment = paymentService.processPayment(orderId, paymentDTO);
        return new ResponseEntity<>(savedPayment, HttpStatus.CREATED);
    }

    @PostMapping("/razorpay/order")
    @PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')")
    public ResponseEntity<RazorpayOrderResponseDTO> createRazorpayOrder(
            @Validated @RequestBody RazorpayOrderRequestDTO requestDTO) {

        try {
            RazorpayOrderResponseDTO response = paymentService.createRazorpayOrder(requestDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Failed to create Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/razorpay/capture")
    @PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')")
    public ResponseEntity<PaymentDTO> captureRazorpayPayment(
            @Validated @RequestBody RazorpayPaymentCaptureRequestDTO requestDTO) {

        try {
            PaymentDTO paymentDTO = paymentService.captureRazorpayPayment(requestDTO);
            return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error capturing Razorpay payment", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or #paymentId == @paymentController.getPaymentOwnerId(#paymentId)")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long paymentId) {
        return new ResponseEntity<>(paymentService.getPaymentById(paymentId), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        return new ResponseEntity<>(paymentService.getAllPayments(), HttpStatus.OK);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByOrderId(@PathVariable Long orderId) {
        return new ResponseEntity<>(paymentService.getPaymentsByOrderId(orderId), HttpStatus.OK);
    }

    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.deletePayment(paymentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
