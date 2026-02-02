package com.example.Ecomm.controller;

import com.example.Ecomm.dto.WishlistItemDTO;
import com.example.Ecomm.service.WishlistService;
import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')") 
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService; 
    private Long getAuthenticatedCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated.");
        }
        
        
        String authenticatedUsername = authentication.getName();
        UserDTO userDTO = userService.getUserByUserName(authenticatedUsername);
        
        return userDTO.getId();
    }


    @GetMapping
    public ResponseEntity<List<WishlistItemDTO>> getWishlist() { 
        Long userId = getAuthenticatedCustomerId();
        List<WishlistItemDTO> wishlist = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping
    public ResponseEntity<WishlistItemDTO> addItemToWishlist(
            @RequestBody Map<String, Long> request) { 
        
        Long userId = getAuthenticatedCustomerId();
        Long productId = request.get("productId");

        if (productId == null) {
            throw new IllegalArgumentException("Product ID must be provided in the request body.");
        }

        WishlistItemDTO newWishlistItem = wishlistService.addItemToWishlist(userId, productId);
        return new ResponseEntity<>(newWishlistItem, HttpStatus.CREATED);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeItemFromWishlist(
            @PathVariable Long productId) { 
        
        Long userId = getAuthenticatedCustomerId();
        wishlistService.removeItemFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }
}
