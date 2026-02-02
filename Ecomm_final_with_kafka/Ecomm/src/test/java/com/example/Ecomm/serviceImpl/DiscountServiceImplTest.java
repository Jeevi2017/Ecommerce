package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.CouponCheckResponseDTO;
import com.example.Ecomm.dto.DiscountDTO;
import com.example.Ecomm.entitiy.Discount;
import com.example.Ecomm.entitiy.Discount.DiscountType;
import com.example.Ecomm.repository.DiscountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountServiceImplTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private Discount discount;
    private DiscountDTO discountDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        discount = new Discount();
        discount.setId(1L);
        discount.setCode("TEST10");
        discount.setType(DiscountType.FIXED_AMOUNT);
        discount.setValue(BigDecimal.valueOf(10));
        discount.setMinOrderAmount(BigDecimal.valueOf(50));
        discount.setStartDate(LocalDateTime.now().minusDays(1));
        discount.setEndDate(LocalDateTime.now().plusDays(1));
        discount.setUsageLimit(10);
        discount.setUsedCount(0);
        discount.setActive(true);

        discountDTO = new DiscountDTO();
        discountDTO.setId(1L);
        discountDTO.setCode("TEST10");
        discountDTO.setType("FIXED_AMOUNT");
        discountDTO.setValue(BigDecimal.valueOf(10));
        discountDTO.setMinOrderAmount(BigDecimal.valueOf(50));
        discountDTO.setStartDate(discount.getStartDate());
        discountDTO.setEndDate(discount.getEndDate());
        discountDTO.setUsageLimit(10);
        discountDTO.setActive(true);
    }

    @Test
    void testCreateDiscount() {
        when(discountRepository.findByCode("TEST10")).thenReturn(Optional.empty());
        when(discountRepository.save(any(Discount.class))).thenReturn(discount);

        DiscountDTO result = discountService.createDiscount(discountDTO);

        assertNotNull(result);
        assertEquals("TEST10", result.getCode());
    }

    @Test
    void testGetDiscountById() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));

        DiscountDTO result = discountService.getDiscountById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetDiscountByCode() {
        when(discountRepository.findByCode("TEST10")).thenReturn(Optional.of(discount));

        DiscountDTO result = discountService.getDiscountByCode("TEST10");

        assertNotNull(result);
        assertEquals("TEST10", result.getCode());
    }

    @Test
    void testGetAllDiscounts() {
        when(discountRepository.findAll()).thenReturn(List.of(discount));

        List<DiscountDTO> result = discountService.getAllDiscounts();

        assertEquals(1, result.size());
    }

    @Test
    void testUpdateDiscount() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
        when(discountRepository.findByCode("TEST10")).thenReturn(Optional.empty());
        when(discountRepository.save(any(Discount.class))).thenReturn(discount);

        DiscountDTO result = discountService.updateDiscount(1L, discountDTO);

        assertNotNull(result);
        assertEquals("TEST10", result.getCode());
    }

    @Test
    void testDeleteDiscount() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
        doNothing().when(discountRepository).delete(discount);

        assertDoesNotThrow(() -> discountService.deleteDiscount(1L));
        verify(discountRepository).delete(discount);
    }

    @Test
    void testIsValidDiscount() {
        when(discountRepository.findByCode("TEST10")).thenReturn(Optional.of(discount));

        boolean result = discountService.isValidDiscount("TEST10", BigDecimal.valueOf(100));

        assertTrue(result);
    }

    @Test
    void testGetAvailableCouponsForCustomer() {
        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        List<DiscountDTO> result = discountService.getAvailableCouponsForCustomer(1L);

        assertEquals(1, result.size());
    }

    @Test
    void testCheckCouponValidityAndUsage() {
        when(discountRepository.findByCode("TEST10")).thenReturn(Optional.of(discount));

        CouponCheckResponseDTO response =
                discountService.checkCouponValidityAndUsage("TEST10", BigDecimal.valueOf(100), 1L);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals("TEST10", response.getCouponCode());
    }

    @Test
    void testIsCodeDuplicate() {
        when(discountRepository.findByCode("TEST10")).thenReturn(Optional.of(discount));

        assertTrue(discountService.isCodeDuplicate("TEST10"));
    }
}
