package com.example.Ecomm.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.example.Ecomm.entitiy.ProductSize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // 🔥 KEY LINE
public class ProductDTO {

    private Long id;
    private String name;
    private String description;
    private List<String> images;
    private BigDecimal price;

    private Long categoryId;
    private String categoryName;

    private Long stockQuantity;

    // ✅ Sizes
    private Set<ProductSize> availableSizes;

    public ProductDTO() {}

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Long stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Set<ProductSize> getAvailableSizes() {
        return availableSizes;
    }

    public void setAvailableSizes(Set<ProductSize> availableSizes) {
        this.availableSizes = availableSizes;
    }
}
