package com.example.Ecomm.service;

import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.entitiy.Cart;

public interface CartService {

    Cart getOrCreateCart(Long customerId);

    CartDTO getCartByCustomerId(Long customerId);

    CartDTO getCartById(Long cartId);

    CartDTO addProductToCart(
            Long customerId,
            Long productId,
            String size,
            Long quantity
    );

    CartDTO updateProductQuantityInCart(
            Long customerId,
            Long productId,
            String size,
            Long newQuantity
    );

    CartDTO removeProductFromCart(
            Long customerId,
            Long productId,
            String size
    );

    void clearCart(Long customerId);

    CartDTO applyCouponToCart(Long customerId, String couponCode);

    CartDTO removeCouponFromCart(Long customerId);
}
