package com.example.Ecomm.service;

import com.example.Ecomm.dto.WishlistItemDTO;
import java.util.List;

public interface WishlistService {

    
    WishlistItemDTO addItemToWishlist(Long userId, Long productId);

    
    List<WishlistItemDTO> getWishlist(Long userId);

    
    void removeItemFromWishlist(Long userId, Long productId);
}
