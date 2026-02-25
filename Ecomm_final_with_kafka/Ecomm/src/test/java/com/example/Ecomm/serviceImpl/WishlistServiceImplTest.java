package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.WishlistItemDTO;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.entitiy.WishlistItem;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CustomerRepository;
import com.example.Ecomm.repository.ProductRepository;
import com.example.Ecomm.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    // ---------- HELPERS ----------

    private Customer mockCustomer() {
        Customer c = new Customer();
        c.setId(1L);
        return c;
    }

    private Product mockProduct() {
        Product p = new Product();
        p.setId(10L);
        p.setName("Shoes");
        p.setPrice(BigDecimal.valueOf(1999));
        return p;
    }

    private WishlistItem mockWishlistItem(Customer customer, Product product) {
        WishlistItem item = new WishlistItem(customer, product);
        item.setId(100L);
        return item;
    }

    // ---------- TESTS ----------

    @Test
    void addItemToWishlist_success() {
        Customer customer = mockCustomer();
        Product product = mockProduct();
        WishlistItem wishlistItem = mockWishlistItem(customer, product);

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));
        when(wishlistRepository.findByCustomerIdAndProductId(1L, 10L))
                .thenReturn(Optional.empty());
        when(wishlistRepository.save(any()))
                .thenReturn(wishlistItem);

        WishlistItemDTO dto = wishlistService.addItemToWishlist(1L, 10L);

        assertNotNull(dto);
        assertEquals(10L, dto.getProductId());
        assertEquals("Shoes", dto.getProductName());

        verify(wishlistRepository).save(any(WishlistItem.class));
    }

    @Test
    void getWishlist_success() {
        Customer customer = mockCustomer();
        Product product = mockProduct();
        WishlistItem wishlistItem = mockWishlistItem(customer, product);

        when(customerRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByCustomerId(1L))
                .thenReturn(List.of(wishlistItem));

        List<WishlistItemDTO> result = wishlistService.getWishlist(1L);

        assertEquals(1, result.size());
        assertEquals("Shoes", result.get(0).getProductName());
    }

    @Test
    void removeItemFromWishlist_success() {
        when(wishlistRepository.findByCustomerIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(new WishlistItem()));

        assertDoesNotThrow(() ->
                wishlistService.removeItemFromWishlist(1L, 10L)
        );

        verify(wishlistRepository)
                .deleteByCustomerIdAndProductId(1L, 10L);
    }

    // ---------- NEGATIVE (OPTIONAL BUT SAFE) ----------

    @Test
    void getWishlist_userNotFound() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                wishlistService.getWishlist(99L)
        );
    }
}
