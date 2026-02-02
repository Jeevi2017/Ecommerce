package com.example.Ecomm.controller;

import com.example.Ecomm.dto.DiscountDTO;
import com.example.Ecomm.dto.ApplyCouponRequest;
import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.dto.CouponCheckResponseDTO;
import com.example.Ecomm.service.DiscountService;
import com.example.Ecomm.service.CartService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/discounts")
@CrossOrigin(origins = "http://localhost:4200")
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    @Autowired
    private CartService cartService;

    // --- Admin CRUD Endpoints ---

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DiscountDTO> createDiscount(@Validated @RequestBody DiscountDTO discountDTO) {
        DiscountDTO createdDiscount = discountService.createDiscount(discountDTO);
        return new ResponseEntity<>(createdDiscount, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DiscountDTO> getDiscountById(@PathVariable Long id) {
        DiscountDTO discount = discountService.getDiscountById(id);
        return new ResponseEntity<>(discount, HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DiscountDTO>> getAllDiscounts() {
        List<DiscountDTO> discounts = discountService.getAllDiscounts();
        return new ResponseEntity<>(discounts, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DiscountDTO> updateDiscount(@PathVariable Long id, @Validated @RequestBody DiscountDTO discountDTO) {
        DiscountDTO updatedDiscount = discountService.updateDiscount(id, discountDTO);
        return new ResponseEntity<>(updatedDiscount, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        discountService.deleteDiscount(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     */
    @GetMapping("/check-duplicate/{code}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // <-- Restored and secured for Admin
    public ResponseEntity<Boolean> checkDuplicateCode(@PathVariable String code) {
        boolean isDuplicate = discountService.isCodeDuplicate(code); 
        return ResponseEntity.ok(isDuplicate);
    }


    // --- Customer Cart/Coupon Management Endpoints ---

    @PostMapping("/apply-coupon/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') and #customerId == authentication.principal.id") // Added principal check
    public ResponseEntity<CartDTO> applyCoupon(@PathVariable Long customerId, @RequestBody ApplyCouponRequest request) {
        try {
            CartDTO updatedCart = cartService.applyCouponToCart(customerId, request.getCouponCode());
            return new ResponseEntity<>(updatedCart, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // Return BAD_REQUEST with the exception message in the body (or headers) for the frontend
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/remove-coupon/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') and #customerId == authentication.principal.id") // Added principal check
    public ResponseEntity<CartDTO> removeCoupon(@PathVariable Long customerId) {
        try {
            CartDTO updatedCart = cartService.removeCouponFromCart(customerId);
            return new ResponseEntity<>(updatedCart, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/available-for-customer/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') and #customerId == authentication.principal.id") // Added principal check
    public ResponseEntity<List<DiscountDTO>> getAvailableCouponsForCustomer(@PathVariable Long customerId) {
        List<DiscountDTO> availableCoupons = discountService.getAvailableCouponsForCustomer(customerId);
        return new ResponseEntity<>(availableCoupons, HttpStatus.OK);
    }

  
    @GetMapping("/check-usage/{code}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<CouponCheckResponseDTO> checkCouponUsage(
            @PathVariable String code,
            @RequestParam("amount") BigDecimal currentAmount) {

        // Extract the authenticated customer ID from the Security Context
        Long customerId = null;
        try {
             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
             // Assuming your Principal object (or the name property) holds the Customer ID (Long)
             // NOTE: Adjust this logic based on how your Security config stores the principal ID.
             if (authentication != null && authentication.getPrincipal() != null) {
                 // Common case: Principal is a UserDetails object where the ID might be a property
                 // or, less securely, if the name is the ID.
                 try {
                     // Attempt to get the ID, assuming it's available or stored as the name.
                     customerId = Long.parseLong(authentication.getName());
                 } catch (NumberFormatException e) {
                     // If parsing fails, the user is authenticated but we can't get the ID easily.
                     // The service must handle a null customerId gracefully.
                 }
             }
        } catch (Exception e) {
             System.err.println("Error retrieving authenticated customer ID: " + e.getMessage());
             // Forcing BAD_REQUEST if authentication state is invalid
             return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        // Use the service to perform the comprehensive validity and usage check
        CouponCheckResponseDTO response = 
            discountService.checkCouponValidityAndUsage(code, currentAmount, customerId);

        // Based on the response, return OK (200) regardless of validity, allowing the 
        // frontend to read the 'isValid' and 'message' fields.
        return ResponseEntity.ok(response);
    }
}
