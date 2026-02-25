package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.OrderService;
import com.example.Ecomm.service.UserService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    // ✅ Constructor Injection (SonarQube compliant)
    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    // ================= AUTH HELPERS =================

    public Long getAuthenticatedCustomerId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserDTO userDTO = userService.getUserByUserName(username);
        return userDTO.getId();
    }

    public Long getCustomerIdByOrderId(Long orderId) {
        OrderDTO orderDTO = orderService.getOrderById(orderId);
        if (orderDTO != null && orderDTO.getCustomerId() != null) {
            return orderDTO.getCustomerId();
        }
        throw new ResourceNotFoundException("Order", "Id", orderId);
    }

    // ================= ADMIN APIs =================

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable Long orderId,
            @Validated @RequestBody OrderDTO orderDTO) {

        return ResponseEntity.ok(orderService.updateOrderFull(orderId, orderDTO));
    }

    // ================= CUSTOMER APIs =================

    @GetMapping("/{orderId}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "@orderController.getAuthenticatedCustomerId() == " +
                    "@orderController.getCustomerIdByOrderId(#orderId)"
    )
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "#customerId == @orderController.getAuthenticatedCustomerId()"
    )
    public ResponseEntity<List<OrderDTO>> getOrdersByCustomerId(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    // ================= ✅ ONE-STEP CHECKOUT =================

    @PostMapping("/from-cart/{customerId}/{addressId}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "') and " +
                    "#customerId == @orderController.getAuthenticatedCustomerId()"
    )
    public ResponseEntity<OrderDTO> createOrderFromCart(
            @PathVariable Long customerId,
            @PathVariable Long addressId) {

        OrderDTO order = orderService.placeOrder(customerId, addressId);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
}
