package com.example.Ecomm.dto;

import java.math.BigDecimal;

public class CouponCheckResponseDTO {

 private String couponCode;
 private boolean isValid; // General validity (active, dates, min amount, etc.)
 private boolean isUsed;  // Specifically, if the coupon is used up (globally or by the customer)
 private String message;
 
 // Optional fields to send back for a successful coupon
 private String discountType;
 private BigDecimal discountValue;

 // Getters and Setters...

 public String getCouponCode() {
     return couponCode;
 }

 public void setCouponCode(String couponCode) {
     this.couponCode = couponCode;
 }

 public boolean isValid() {
     return isValid;
 }

 public void setValid(boolean isValid) {
     this.isValid = isValid;
 }

 public boolean isUsed() {
     return isUsed;
 }

 public void setUsed(boolean isUsed) {
     this.isUsed = isUsed;
 }

 public String getMessage() {
     return message;
 }

 public void setMessage(String message) {
     this.message = message;
 }

 public String getDiscountType() {
     return discountType;
 }

 public void setDiscountType(String discountType) {
     this.discountType = discountType;
 }

 public BigDecimal getDiscountValue() {
     return discountValue;
 }

 public void setDiscountValue(BigDecimal discountValue) {
     this.discountValue = discountValue;
 }
}