package com.example.Ecomm.dto;

public class RazorpayOrderResponseDTO {

    private String id; 
    private String entity; 
    private Long amount; 
    private String currency;
    private String receipt;
    private String status; 
    private int attempts;
    private Long createdAt; 

    public RazorpayOrderResponseDTO() {}

    public RazorpayOrderResponseDTO(String id, String entity, Long amount, String currency, String receipt, String status, int attempts, Long createdAt) {
        this.id = id;
        this.entity = entity;
        this.amount = amount;
        this.currency = currency;
        this.receipt = receipt;
        this.status = status;
        this.attempts = attempts;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
