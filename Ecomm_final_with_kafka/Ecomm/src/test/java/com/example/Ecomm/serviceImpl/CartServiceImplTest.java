package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.CartDTO;
import com.example.Ecomm.entitiy.*;
import com.example.Ecomm.entitiy.Discount.DiscountType;
import com.example.Ecomm.repository.*;
import com.example.Ecomm.service.DiscountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @InjectMocks
    private CartServiceImpl cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private DiscountService discountService;

    // ---------- HELPERS ----------

    private Customer mockCustomer() {
        Customer c = new Customer();
        c.setId(1L);
        return c;
    }

    private Cart mockCart(Customer customer) {
        Cart cart = new Cart();
        cart.setCustomer(customer);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        return cart;
    }

    private Product mockProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("T-Shirt");
        p.setPrice(BigDecimal.valueOf(500));
        p.setStockQuantity(10L);
        return p;
    }

    // ---------- TESTS ----------

    @Test
    void getOrCreateCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        Cart result = cartService.getOrCreateCart(1L);

        assertNotNull(result);
    }

    @Test
    void getCartById() {
        Cart cart = mockCart(mockCustomer());
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));

        CartDTO dto = cartService.getCartById(1L);
        assertNotNull(dto);
    }

    @Test
    void getCartByCustomerId() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        assertNotNull(cartService.getCartByCustomerId(1L));
    }

    @Test
    void addProductToCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);
        Product product = mockProduct();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any())).thenReturn(cart);

        CartDTO dto = cartService.addProductToCart(1L, 1L, "M", 1L);

        assertNotNull(dto);
    }

    @Test
    void updateProductQuantityInCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);
        Product product = mockProduct();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1L);
        item.setSize("M");
        item.setPrice(BigDecimal.valueOf(500));
        cart.addCartItem(item);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        CartDTO dto = cartService.updateProductQuantityInCart(1L, 1L, "M", 2L);
        assertNotNull(dto);
    }

    @Test
    void removeProductFromCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);
        Product product = mockProduct();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1L);
        item.setSize("M");
        item.setPrice(BigDecimal.valueOf(500));
        cart.addCartItem(item);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        CartDTO dto = cartService.removeProductFromCart(1L, 1L, "M");
        assertNotNull(dto);
    }

    @Test
    void clearCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        assertDoesNotThrow(() -> cartService.clearCart(1L));
    }

    @Test
    void applyCouponToCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);

        Discount discount = new Discount();
        discount.setCode("SAVE10");
        discount.setType(DiscountType.PERCENTAGE);   // ✅ FINAL FIX
        discount.setValue(BigDecimal.valueOf(10));   // 10%

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("SAVE10")).thenReturn(Optional.of(discount));
        when(discountService.isValidDiscount(any(), any())).thenReturn(true);
        when(cartRepository.save(any())).thenReturn(cart);

        CartDTO dto = cartService.applyCouponToCart(1L, "SAVE10");
        assertNotNull(dto);
    }

    @Test
    void removeCouponFromCart() {
        Customer customer = mockCustomer();
        Cart cart = mockCart(customer);
        cart.setCouponCode("SAVE10");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        CartDTO dto = cartService.removeCouponFromCart(1L);
        assertNotNull(dto);
        assertNull(dto.getCouponCode());
    }
}
