package com.example.Ecomm.controller;

import com.example.Ecomm.dto.*;
import com.example.Ecomm.service.CartService;
import com.example.Ecomm.service.DiscountService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountControllerTest {

    @InjectMocks
    private DiscountController discountController;

    @Mock
    private DiscountService discountService;

    @Mock
    private CartService cartService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- HELPERS ----------

    private DiscountDTO mockDiscountDTO() {
        DiscountDTO dto = new DiscountDTO();
        dto.setId(1L);
        dto.setCode("SAVE10");
        return dto;
    }

    private CartDTO mockCartDTO() {
        CartDTO cart = new CartDTO();
        cart.setId(1L);
        return cart;
    }

    // ---------- ADMIN TESTS ----------

    @Test
    void createDiscount() {
        when(discountService.createDiscount(any()))
                .thenReturn(mockDiscountDTO());

        ResponseEntity<DiscountDTO> response =
                discountController.createDiscount(mockDiscountDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getDiscountById() {
        when(discountService.getDiscountById(1L))
                .thenReturn(mockDiscountDTO());

        ResponseEntity<DiscountDTO> response =
                discountController.getDiscountById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SAVE10", response.getBody().getCode());
    }

    @Test
    void getAllDiscounts() {
        when(discountService.getAllDiscounts())
                .thenReturn(List.of(mockDiscountDTO()));

        ResponseEntity<List<DiscountDTO>> response =
                discountController.getAllDiscounts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateDiscount() {
        when(discountService.updateDiscount(eq(1L), any()))
                .thenReturn(mockDiscountDTO());

        ResponseEntity<DiscountDTO> response =
                discountController.updateDiscount(1L, mockDiscountDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteDiscount() {
        doNothing().when(discountService).deleteDiscount(1L);

        ResponseEntity<Void> response =
                discountController.deleteDiscount(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void checkDuplicateCode() {
        when(discountService.isCodeDuplicate("SAVE10"))
                .thenReturn(true);

        ResponseEntity<Boolean> response =
                discountController.checkDuplicateCode("SAVE10");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
    }

    // ---------- CUSTOMER / CART TESTS ----------

    @Test
    void applyCoupon() {
        ApplyCouponRequest request = new ApplyCouponRequest();
        request.setCouponCode("SAVE10");

        when(cartService.applyCouponToCart(1L, "SAVE10"))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                discountController.applyCoupon(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void removeCoupon() {
        when(cartService.removeCouponFromCart(1L))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                discountController.removeCoupon(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAvailableCouponsForCustomer() {
        when(discountService.getAvailableCouponsForCustomer(1L))
                .thenReturn(List.of(mockDiscountDTO()));

        ResponseEntity<List<DiscountDTO>> response =
                discountController.getAvailableCouponsForCustomer(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void checkCouponUsage() {
        // Fake authenticated user (name = customerId)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null)
        );

        CouponCheckResponseDTO checkResponse = new CouponCheckResponseDTO();
        checkResponse.setValid(true);
        checkResponse.setMessage("Coupon valid");

        when(discountService.checkCouponValidityAndUsage(
                eq("SAVE10"),
                eq(BigDecimal.valueOf(500)),
                eq(1L)
        )).thenReturn(checkResponse);

        ResponseEntity<CouponCheckResponseDTO> response =
                discountController.checkCouponUsage(
                        "SAVE10",
                        BigDecimal.valueOf(500)
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isValid());
    }
}
