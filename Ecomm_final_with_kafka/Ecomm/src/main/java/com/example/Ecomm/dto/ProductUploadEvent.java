package com.example.Ecomm.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductUploadEvent {
    private String batchId; 
    private String name;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private Long stockQuantity;
    private List<String> imageUrls;

    public ProductUploadEvent() {}

    public ProductUploadEvent(String batchId, String name, String description, BigDecimal price, Long categoryId, Integer stockQuantity, List<String> imageUrls) {
        this.batchId = batchId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.stockQuantity = Long.valueOf(stockQuantity); 
        this.imageUrls = imageUrls;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getStockQuantity() {
        return stockQuantity;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setStockQuantity(Long stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @Override
    public String toString() {
        return "ProductUploadEvent{" +
               "batchId='" + batchId + '\'' +
               ", name='" + name + '\'' +
               ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 50)) + "..." : "null") + '\'' +
               ", price=" + price +
               ", categoryId=" + categoryId +
               ", stockQuantity=" + stockQuantity +
               ", imageUrls=" + imageUrls +
               '}';
    }
}
