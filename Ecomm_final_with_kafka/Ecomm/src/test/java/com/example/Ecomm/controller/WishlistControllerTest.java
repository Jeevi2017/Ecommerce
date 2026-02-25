package com.example.Ecomm.controller;

import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.dto.WishlistItemDTO;
import com.example.Ecomm.service.UserService;
import com.example.Ecomm.service.WishlistService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    @InjectMocks
    private WishlistController wishlistController;

    @Mock
    private WishlistService wishlistService;

    @Mock
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- HELPERS ----------

    private void mockAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "testuser",
                        null,
                        List.of() // ✅ marks authentication as authenticated
                )
        );

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername("testuser");

        when(userService.getUserByUserName("testuser"))
                .thenReturn(userDTO);
    }

    private WishlistItemDTO mockWishlistItem() {
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setProductId(10L);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getWishlist() {
        mockAuthenticatedUser(1L);

        when(wishlistService.getWishlist(1L))
                .thenReturn(List.of(mockWishlistItem()));

        ResponseEntity<List<WishlistItemDTO>> response =
                wishlistController.getWishlist();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());

        verify(wishlistService).getWishlist(1L);
    }

    @Test
    void addItemToWishlist() {
        mockAuthenticatedUser(1L);

        when(wishlistService.addItemToWishlist(1L, 10L))
                .thenReturn(mockWishlistItem());

        ResponseEntity<WishlistItemDTO> response =
                wishlistController.addItemToWishlist(
                        Map.of("productId", 10L)
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(wishlistService).addItemToWishlist(1L, 10L);
    }

    @Test
    void removeItemFromWishlist() {
        mockAuthenticatedUser(1L);

        doNothing().when(wishlistService)
                .removeItemFromWishlist(1L, 10L);

        ResponseEntity<Void> response =
                wishlistController.removeItemFromWishlist(10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(wishlistService).removeItemFromWishlist(1L, 10L);
    }
}
