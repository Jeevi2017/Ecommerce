package com.example.Ecomm.controller;

import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.dto.CartItemDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.service.CartService;
import com.example.Ecomm.service.CustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @InjectMocks
    private CartController cartController;

    @Mock
    private CartService cartService;

    @Mock
    private CustomerService customerService;

    @BeforeEach
    void setupAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- HELPERS ----------

    private CartDTO mockCartDTO() {
        CartDTO cart = new CartDTO();
        cart.setId(1L);
        cart.setCustomerId(1L);
        return cart;
    }

    // ---------- TESTS ----------

    @Test
    void getAuthenticatedCustomerId() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");

        when(customerService.getCustomerByUsername("testuser"))
                .thenReturn(userDTO);

        Long customerId = cartController.getAuthenticatedCustomerId();

        assertEquals(1L, customerId);
    }

    @Test
    void getCartByCustomerId() {
        when(cartService.getCartByCustomerId(1L))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                cartController.getCartByCustomerId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void addProductToCart() {
        CartItemDTO itemDTO = new CartItemDTO();
        itemDTO.setProductId(10L);
        itemDTO.setSize("M");
        itemDTO.setQuantity(2L);

        when(cartService.addProductToCart(1L, 10L, "M", 2L))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                cartController.addProductToCart(1L, itemDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateProductQuantityInCart() {
        when(cartService.updateProductQuantityInCart(1L, 10L, "M", 3L))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                cartController.updateProductQuantityInCart(1L, 10L, "M", 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void removeProductFromCart() {
        when(cartService.removeProductFromCart(1L, 10L, "M"))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                cartController.removeProductFromCart(1L, 10L, "M");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void clearCart() {
        doNothing().when(cartService).clearCart(1L);

        ResponseEntity<Void> response =
                cartController.clearCart(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getCartById() {
        when(cartService.getCartById(1L))
                .thenReturn(mockCartDTO());

        ResponseEntity<CartDTO> response =
                cartController.getCartById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
