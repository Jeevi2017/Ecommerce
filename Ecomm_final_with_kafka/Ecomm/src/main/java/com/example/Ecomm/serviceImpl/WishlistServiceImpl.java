package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.WishlistItemDTO;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.entitiy.WishlistItem;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CustomerRepository;
import com.example.Ecomm.repository.ProductRepository;
import com.example.Ecomm.repository.WishlistRepository;
import com.example.Ecomm.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private CustomerRepository customerRepository; 
    
    @Autowired
    private ProductRepository productRepository; 

    @Override
    @Transactional
    public WishlistItemDTO addItemToWishlist(Long userId, Long productId) {
        
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // 2. Check for duplicates
        if (wishlistRepository.findByCustomerIdAndProductId(userId, productId).isPresent()) {
            // IllegalArgumentException caught by GlobalExceptionHandler (400)
            throw new IllegalArgumentException("Product ID " + productId + " is already in the wishlist.");
        }

        // 3. Create and Save
        WishlistItem wishlistItem = new WishlistItem(customer, product);
        WishlistItem savedItem = wishlistRepository.save(wishlistItem);

        return entityToDto(savedItem);
    }

    @Override
    public List<WishlistItemDTO> getWishlist(Long userId) {
        // We only check if the user exists if the repository returns an empty list, 
        // to return a 404 if the user ID is genuinely invalid.
        if (!customerRepository.existsById(userId)) {
             throw new ResourceNotFoundException("User", "id", userId);
        }
        
        List<WishlistItem> wishlistItems = wishlistRepository.findByCustomerId(userId);
        return wishlistItems.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeItemFromWishlist(Long userId, Long productId) {
        // 1. Check if the item exists for the user
        if (wishlistRepository.findByCustomerIdAndProductId(userId, productId).isEmpty()) {
            // ResourceNotFoundException caught by GlobalExceptionHandler (404)
            throw new ResourceNotFoundException(
                "WishlistItem", 
                "userId and productId", 
                userId + " & " + productId
            );
        }
        // 2. Delete the item
        wishlistRepository.deleteByCustomerIdAndProductId(userId, productId);
    }

    // --- Mapper methods ---

    private WishlistItemDTO entityToDto(WishlistItem item) {
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductPrice(item.getProduct().getPrice());
        dto.setAddedAt(item.getAddedAt());
        return dto;
    }
}
