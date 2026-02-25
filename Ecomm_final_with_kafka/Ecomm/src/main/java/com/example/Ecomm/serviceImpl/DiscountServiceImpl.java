package com.example.Ecomm.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.Ecomm.dto.CouponCheckResponseDTO;
import com.example.Ecomm.dto.DiscountDTO;
import com.example.Ecomm.entitiy.Discount;
import com.example.Ecomm.entitiy.Discount.DiscountType;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.DiscountRepository;
import com.example.Ecomm.service.DiscountService;

import jakarta.transaction.Transactional;

@Service
public class DiscountServiceImpl implements DiscountService {

    private static final String DISCOUNT_ENTITY = "Discount";

    private final DiscountRepository discountRepository;

    // ✅ Constructor injection
    public DiscountServiceImpl(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @Override
    @Transactional
    public DiscountDTO createDiscount(DiscountDTO discountDTO) {
        if (discountRepository.findByCode(discountDTO.getCode()).isPresent()) {
            throw new IllegalArgumentException(
                    DISCOUNT_ENTITY + " code '" + discountDTO.getCode() + "' already exists.");
        }

        Discount discount = mapDTOToDiscount(discountDTO);
        discount.setUsedCount(0);
        return mapDiscountToDTO(discountRepository.save(discount));
    }

    @Override
    public DiscountDTO getDiscountById(Long id) {
        return mapDiscountToDTO(
                discountRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(DISCOUNT_ENTITY, "id", id))
        );
    }

    @Override
    public DiscountDTO getDiscountByCode(String code) {
        return mapDiscountToDTO(
                discountRepository.findByCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException(DISCOUNT_ENTITY, "code", code))
        );
    }

    @Override
    public List<DiscountDTO> getAllDiscounts() {
        return discountRepository.findAll()
                .stream()
                .map(this::mapDiscountToDTO)
                .toList();
    }

    @Override
    @Transactional
    public DiscountDTO updateDiscount(Long id, DiscountDTO discountDTO) {
        Discount existingDiscount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DISCOUNT_ENTITY, "id", id));

        if (!existingDiscount.getCode().equals(discountDTO.getCode())
                && discountRepository.findByCode(discountDTO.getCode()).isPresent()) {
            throw new IllegalArgumentException(
                    DISCOUNT_ENTITY + " code '" + discountDTO.getCode() + "' already exists.");
        }

        existingDiscount.setCode(discountDTO.getCode());
        existingDiscount.setType(DiscountType.valueOf(discountDTO.getType()));
        existingDiscount.setValue(discountDTO.getValue());
        existingDiscount.setMinOrderAmount(discountDTO.getMinOrderAmount());
        existingDiscount.setStartDate(discountDTO.getStartDate());
        existingDiscount.setEndDate(discountDTO.getEndDate());
        existingDiscount.setUsageLimit(discountDTO.getUsageLimit());
        existingDiscount.setActive(discountDTO.isActive());

        return mapDiscountToDTO(discountRepository.save(existingDiscount));
    }

    @Override
    @Transactional
    public void deleteDiscount(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DISCOUNT_ENTITY, "id", id));
        discountRepository.delete(discount);
    }

    @Override
    public boolean isValidDiscount(String code, BigDecimal currentAmount) {
        return discountRepository.findByCode(code)
                .map(discount -> {
                    LocalDateTime now = LocalDateTime.now();
                    return discount.isActive()
                            && !now.isBefore(discount.getStartDate())
                            && !now.isAfter(discount.getEndDate())
                            && (discount.getUsageLimit() == null
                            || discount.getUsedCount() < discount.getUsageLimit())
                            && (discount.getMinOrderAmount() == null
                            || currentAmount.compareTo(discount.getMinOrderAmount()) >= 0);
                })
                .orElse(false);
    }

    @Override
    public List<DiscountDTO> getAvailableCouponsForCustomer(Long customerId) {
        LocalDateTime now = LocalDateTime.now();

        return discountRepository.findByActiveTrue()
                .stream()
                .filter(discount ->
                        !now.isBefore(discount.getStartDate())
                                && !now.isAfter(discount.getEndDate())
                                && (discount.getUsageLimit() == null
                                || discount.getUsedCount() < discount.getUsageLimit())
                )
                .map(this::mapDiscountToDTO)
                .toList();
    }

    @Override
    public CouponCheckResponseDTO checkCouponValidityAndUsage(
            String code, BigDecimal currentAmount, Long customerId) {

        CouponCheckResponseDTO response = new CouponCheckResponseDTO();
        response.setCouponCode(code);

        Optional<Discount> discountOpt = discountRepository.findByCode(code);
        if (discountOpt.isEmpty()) {
            response.setValid(false);
            response.setUsed(false);
            response.setMessage("Coupon code does not exist.");
            return response;
        }

        Discount discount = discountOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            response.setValid(false);
            response.setUsed(true);
            response.setMessage("Coupon has reached its maximum usage limit globally.");
            return response;
        }

        if (!discount.isActive()) {
            response.setValid(false);
            response.setMessage("Coupon is not currently active.");
        } else if (now.isBefore(discount.getStartDate()) || now.isAfter(discount.getEndDate())) {
            response.setValid(false);
            response.setMessage("Coupon is expired or not yet active.");
        } else if (discount.getMinOrderAmount() != null
                && currentAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            response.setValid(false);
            response.setMessage("Minimum order amount of " + discount.getMinOrderAmount() + " is required.");
        } else {
            response.setValid(true);
            response.setMessage("Coupon is valid.");
        }

        if (response.isValid()) {
            response.setDiscountType(discount.getType().name());
            response.setDiscountValue(discount.getValue());
        }

        return response;
    }

    @Override
    public boolean isCodeDuplicate(String code) {
        return discountRepository.findByCode(code).isPresent();
    }

    // ===================== MAPPERS =====================

    private Discount mapDTOToDiscount(DiscountDTO dto) {
        Discount discount = new Discount();
        discount.setId(dto.getId());
        discount.setCode(dto.getCode());
        discount.setType(DiscountType.valueOf(dto.getType()));
        discount.setValue(dto.getValue());
        discount.setMinOrderAmount(dto.getMinOrderAmount());
        discount.setStartDate(dto.getStartDate());
        discount.setEndDate(dto.getEndDate());
        discount.setUsageLimit(dto.getUsageLimit());
        discount.setUsedCount(dto.getUsedCount() != null ? dto.getUsedCount() : 0);
        discount.setActive(dto.isActive());
        return discount;
    }

    private DiscountDTO mapDiscountToDTO(Discount discount) {
        DiscountDTO dto = new DiscountDTO();
        dto.setId(discount.getId());
        dto.setCode(discount.getCode());
        dto.setType(discount.getType().name());
        dto.setValue(discount.getValue());
        dto.setMinOrderAmount(discount.getMinOrderAmount());
        dto.setStartDate(discount.getStartDate());
        dto.setEndDate(discount.getEndDate());
        dto.setUsageLimit(discount.getUsageLimit());
        dto.setUsedCount(discount.getUsedCount());
        dto.setActive(discount.isActive());
        return dto;
    }
}
