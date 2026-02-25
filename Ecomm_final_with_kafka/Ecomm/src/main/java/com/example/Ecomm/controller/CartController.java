package com.example.Ecomm.controller;

import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.dto.CartItemDTO;
import com.example.Ecomm.service.CartService;
import com.example.Ecomm.service.CustomerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

    private final CartService cartService;
    private final CustomerService customerService;

    // ✅ Constructor Injection (SonarQube compliant)
    public CartController(CartService cartService,
                          CustomerService customerService) {
        this.cartService = cartService;
        this.customerService = customerService;
    }

    // ================= AUTH =================

    public Long getAuthenticatedCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return customerService.getCustomerByUsername(username).getId();
    }

    // ================= GET CART =================

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #customerId == @cartController.getAuthenticatedCustomerId()")
    public ResponseEntity<CartDTO> getCartByCustomerId(@PathVariable Long customerId) {
        CartDTO cart = cartService.getCartByCustomerId(customerId);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    // ================= ADD ITEM (SIZE AWARE) =================

    @PostMapping("/customer/{customerId}/items")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #customerId == @cartController.getAuthenticatedCustomerId()")
    public ResponseEntity<CartDTO> addProductToCart(
            @PathVariable Long customerId,
            @RequestBody CartItemDTO cartItemDTO) {

        CartDTO updatedCart = cartService.addProductToCart(
                customerId,
                cartItemDTO.getProductId(),
                cartItemDTO.getSize(),
                cartItemDTO.getQuantity()
        );

        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    // ================= UPDATE QUANTITY (SIZE AWARE) =================

    @PutMapping("/customer/{customerId}/items/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #customerId == @cartController.getAuthenticatedCustomerId()")
    public ResponseEntity<CartDTO> updateProductQuantityInCart(
            @PathVariable Long customerId,
            @PathVariable Long productId,
            @RequestParam String size,
            @RequestParam Long newQuantity) {

        CartDTO updatedCart = cartService.updateProductQuantityInCart(
                customerId,
                productId,
                size,
                newQuantity
        );

        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    // ================= REMOVE ITEM (SIZE AWARE) =================

    @DeleteMapping("/customer/{customerId}/items/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #customerId == @cartController.getAuthenticatedCustomerId()")
    public ResponseEntity<CartDTO> removeProductFromCart(
            @PathVariable Long customerId,
            @PathVariable Long productId,
            @RequestParam String size) {

        CartDTO updatedCart = cartService.removeProductFromCart(
                customerId,
                productId,
                size
        );

        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    // ================= CLEAR CART =================

    @DeleteMapping("/customer/{customerId}/clear")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #customerId == @cartController.getAuthenticatedCustomerId()")
    public ResponseEntity<Void> clearCart(@PathVariable Long customerId) {
        cartService.clearCart(customerId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ================= ADMIN =================

    @GetMapping("/id/{cartId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CartDTO> getCartById(@PathVariable Long cartId) {
        CartDTO cart = cartService.getCartById(cartId);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }
}
