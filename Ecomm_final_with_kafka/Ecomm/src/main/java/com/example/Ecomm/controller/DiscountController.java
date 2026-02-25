package com.example.Ecomm.controller;

import com.example.Ecomm.dto.DiscountDTO;
import com.example.Ecomm.dto.ApplyCouponRequest;
import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.dto.CouponCheckResponseDTO;
import com.example.Ecomm.service.DiscountService;
import com.example.Ecomm.service.CartService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger =
            LoggerFactory.getLogger(DiscountController.class);

    private final DiscountService discountService;
    private final CartService cartService;

    // ✅ Constructor Injection (SonarQube compliant)
    public DiscountController(DiscountService discountService,
                              CartService cartService) {
        this.discountService = discountService;
        this.cartService = cartService;
    }

    // --- Admin CRUD Endpoints ---

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DiscountDTO> createDiscount(
            @Validated @RequestBody DiscountDTO discountDTO) {

        DiscountDTO createdDiscount =
                discountService.createDiscount(discountDTO);
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
        return new ResponseEntity<>(
                discountService.getAllDiscounts(),
                HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DiscountDTO> updateDiscount(
            @PathVariable Long id,
            @Validated @RequestBody DiscountDTO discountDTO) {

        DiscountDTO updatedDiscount =
                discountService.updateDiscount(id, discountDTO);
        return new ResponseEntity<>(updatedDiscount, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        discountService.deleteDiscount(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/check-duplicate/{code}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Boolean> checkDuplicateCode(
            @PathVariable String code) {

        return ResponseEntity.ok(discountService.isCodeDuplicate(code));
    }

    // --- Customer Cart / Coupon Endpoints ---

    @PostMapping("/apply-coupon/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') and #customerId == authentication.principal.id")
    public ResponseEntity<CartDTO> applyCoupon(
            @PathVariable Long customerId,
            @RequestBody ApplyCouponRequest request) {

        try {
            return ResponseEntity.ok(
                    cartService.applyCouponToCart(
                            customerId, request.getCouponCode()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/remove-coupon/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') and #customerId == authentication.principal.id")
    public ResponseEntity<CartDTO> removeCoupon(@PathVariable Long customerId) {

        try {
            return ResponseEntity.ok(
                    cartService.removeCouponFromCart(customerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/available-for-customer/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') and #customerId == authentication.principal.id")
    public ResponseEntity<List<DiscountDTO>> getAvailableCouponsForCustomer(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                discountService.getAvailableCouponsForCustomer(customerId));
    }

    @GetMapping("/check-usage/{code}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<CouponCheckResponseDTO> checkCouponUsage(
            @PathVariable String code,
            @RequestParam("amount") BigDecimal currentAmount) {

        Long customerId = resolveAuthenticatedCustomerId();
        CouponCheckResponseDTO response =
                discountService.checkCouponValidityAndUsage(
                        code, currentAmount, customerId);

        return ResponseEntity.ok(response);
    }

    // ================= HELPER =================

    // ✅ Extracted nested try block
    private Long resolveAuthenticatedCustomerId() {
        try {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getName() != null) {
                try {
                    return Long.parseLong(authentication.getName());
                } catch (NumberFormatException e) {
                    return null; // Service handles null safely
                }
            }
        } catch (Exception e) {
            logger.error(
                    "Error retrieving authenticated customer ID", e);
        }
        return null;
    }
}
