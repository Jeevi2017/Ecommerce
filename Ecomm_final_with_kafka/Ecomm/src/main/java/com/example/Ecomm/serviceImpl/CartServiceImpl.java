package com.example.Ecomm.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.dto.CartItemDTO;
import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.entitiy.Cart;
import com.example.Ecomm.entitiy.CartItem;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Discount;
import com.example.Ecomm.entitiy.Discount.DiscountType;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CartItemRepository;
import com.example.Ecomm.repository.CartRepository;
import com.example.Ecomm.repository.CustomerRepository;
import com.example.Ecomm.repository.DiscountRepository;
import com.example.Ecomm.repository.ProductRepository;
import com.example.Ecomm.service.CartService;
import com.example.Ecomm.service.DiscountService;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;
    private final DiscountRepository discountRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           DiscountService discountService,
                           DiscountRepository discountRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.discountService = discountService;
        this.discountRepository = discountRepository;
    }

    // ================= CART =================

    @Override
    @Transactional
    public Cart getOrCreateCart(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer", "Id", customerId));

        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    cart.setCreatedAt(LocalDateTime.now());
                    cart.setUpdatedAt(LocalDateTime.now());
                    cart.setTotalAmount(BigDecimal.ZERO);
                    cart.setTotalPrice(BigDecimal.ZERO);
                    cart.setDiscountAmount(BigDecimal.ZERO);
                    cart.setCouponCode(null);
                    return cartRepository.save(cart);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartById(Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart", "Id", cartId));

        recalculateCartTotal(cart);
        return mapCartToDTO(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartByCustomerId(Long customerId) {
        return mapCartToDTO(getOrCreateCart(customerId));
    }

    // ================= ADD ITEM =================

    @Override
    @Transactional
    public CartDTO addProductToCart(Long customerId, Long productId, String size, Long quantity) {

        Cart cart = getOrCreateCart(customerId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "Id", productId));

        if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
        }

        CartItem cartItem = cart.getCartItems().stream()
                .filter(i ->
                        i.getProduct().getId().equals(productId)
                                && i.getSize().equalsIgnoreCase(size))
                .findFirst()
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setCart(cart);
                    item.setProduct(product);
                    item.setQuantity(0L);
                    item.setPrice(product.getPrice());
                    item.setSize(size);
                    cart.addCartItem(item);
                    return item;
                });

        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        product.setStockQuantity(product.getStockQuantity() - quantity);

        cartItemRepository.save(cartItem);
        productRepository.save(product);

        recalculateCartTotal(cart);
        cartRepository.save(cart);

        return mapCartToDTO(cart);
    }

    // ================= UPDATE QUANTITY =================

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long customerId, Long productId, String size, Long newQuantity) {

        Cart cart = getOrCreateCart(customerId);

        CartItem cartItem = cart.getCartItems().stream()
                .filter(i ->
                        i.getProduct().getId().equals(productId)
                                && i.getSize().equalsIgnoreCase(size))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("CartItem", "Product+Size", productId));

        if (newQuantity <= 0) {
            return removeProductFromCart(customerId, productId, size);
        }

        Long difference = newQuantity - cartItem.getQuantity();
        Product product = cartItem.getProduct();

        if (difference > 0 && product.getStockQuantity() < difference) {
            throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
        }

        product.setStockQuantity(product.getStockQuantity() - difference);
        cartItem.setQuantity(newQuantity);

        productRepository.save(product);
        cartItemRepository.save(cartItem);

        recalculateCartTotal(cart);
        cartRepository.save(cart);

        return mapCartToDTO(cart);
    }

    // ================= REMOVE ITEM =================

    @Override
    @Transactional
    public CartDTO removeProductFromCart(Long customerId, Long productId, String size) {

        Cart cart = getOrCreateCart(customerId);

        CartItem cartItem = cart.getCartItems().stream()
                .filter(i ->
                        i.getProduct().getId().equals(productId)
                                && i.getSize().equalsIgnoreCase(size))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("CartItem", "Product+Size", productId));

        Product product = cartItem.getProduct();
        product.setStockQuantity(product.getStockQuantity() + cartItem.getQuantity());

        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);
        productRepository.save(product);

        recalculateCartTotal(cart);
        cartRepository.save(cart);

        return mapCartToDTO(cart);
    }

    // ================= CLEAR CART =================

    @Override
    @Transactional
    public void clearCart(Long customerId) {

        Cart cart = getOrCreateCart(customerId);

        for (CartItem item : new ArrayList<>(cart.getCartItems())) {

            // Restore stock
            Product product = item.getProduct();
            product.setStockQuantity(
                    product.getStockQuantity() + item.getQuantity()
            );
            productRepository.save(product);

            // Delete item properly
            cartItemRepository.delete(item);
        }

        // Clear collection safely
        cart.getCartItems().clear();

        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setCouponCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }


    // ================= COUPONS =================

    @Override
    @Transactional
    public CartDTO applyCouponToCart(Long customerId, String couponCode) {

        Cart cart = getOrCreateCart(customerId);

        Discount discount = discountRepository.findByCode(couponCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Discount", "code", couponCode));

        if (!discountService.isValidDiscount(couponCode, calculateSubtotal(cart))) {
            throw new IllegalArgumentException("Invalid coupon");
        }

        cart.setCouponCode(couponCode);
        recalculateCartTotal(cart);

        return mapCartToDTO(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartDTO removeCouponFromCart(Long customerId) {

        Cart cart = getOrCreateCart(customerId);
        cart.setCouponCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);

        recalculateCartTotal(cart);
        return mapCartToDTO(cartRepository.save(cart));
    }

    // ================= CALCULATIONS =================

    private BigDecimal calculateSubtotal(Cart cart) {

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getCartItems()) {
            subtotal = subtotal.add(
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private void recalculateCartTotal(Cart cart) {

        BigDecimal subtotal = calculateSubtotal(cart);
        cart.setTotalPrice(subtotal);
        cart.setDiscountAmount(BigDecimal.ZERO);

        BigDecimal finalTotal = subtotal;

        if (cart.getCouponCode() != null) {
            Discount discount = discountRepository
                    .findByCode(cart.getCouponCode())
                    .orElse(null);

            if (discount != null && discountService.isValidDiscount(discount.getCode(), subtotal)) {

                BigDecimal discountValue =
                        discount.getType() == DiscountType.PERCENTAGE
                                ? subtotal.multiply(discount.getValue().divide(BigDecimal.valueOf(100)))
                                : discount.getValue();

                cart.setDiscountAmount(discountValue.min(subtotal));
                finalTotal = subtotal.subtract(discountValue);
            }
        }

        cart.setTotalAmount(
                finalTotal.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));

        cart.setUpdatedAt(LocalDateTime.now());
    }

    // ================= MAPPERS =================

    private CartDTO mapCartToDTO(Cart cart) {

        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setCustomerId(cart.getCustomer().getId());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());
        dto.setTotalPrice(cart.getTotalPrice());
        dto.setTotalAmount(cart.getTotalAmount());
        dto.setCouponCode(cart.getCouponCode());
        dto.setDiscountAmount(cart.getDiscountAmount());

        dto.setCartItems(
                cart.getCartItems().stream()
                        .map(this::mapCartItemToDTO)
                        .toList()
        );

        return dto;
    }

    private CartItemDTO mapCartItemToDTO(CartItem item) {

        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSize(item.getSize());

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(item.getProduct().getId());
        productDTO.setName(item.getProduct().getName());
        productDTO.setPrice(item.getProduct().getPrice());
        productDTO.setImages(item.getProduct().getImages());
        productDTO.setDescription(item.getProduct().getDescription());
        productDTO.setStockQuantity(item.getProduct().getStockQuantity());

        if (item.getProduct().getCategory() != null) {
            productDTO.setCategoryId(item.getProduct().getCategory().getId());
            productDTO.setCategoryName(item.getProduct().getCategory().getName());
        }

        dto.setProductDetails(productDTO);
        return dto;
    }
}
