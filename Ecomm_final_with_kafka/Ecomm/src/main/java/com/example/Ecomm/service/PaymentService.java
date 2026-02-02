package com.example.Ecomm.service;

import java.util.List;

import org.json.JSONObject;

import com.example.Ecomm.dto.PaymentDTO;
import com.example.Ecomm.dto.RazorpayOrderRequestDTO;
import com.example.Ecomm.dto.RazorpayOrderResponseDTO;
import com.example.Ecomm.dto.RazorpayPaymentCaptureRequestDTO;

public interface PaymentService {

    
    PaymentDTO processPayment(Long orderId, PaymentDTO paymentDTO);

    
    PaymentDTO getPaymentById(Long paymentId);

    
    List<PaymentDTO> getAllPayments();

   
    List<PaymentDTO> getPaymentsByOrderId(Long orderId);

    
    void deletePayment(Long paymentId);
    
    PaymentDTO captureWebhookPayment(JSONObject paymentEntity, Long internalOrderId) throws Exception;
    
    
    RazorpayOrderResponseDTO createRazorpayOrder(RazorpayOrderRequestDTO requestDTO) throws Exception;

   
    PaymentDTO captureRazorpayPayment(RazorpayPaymentCaptureRequestDTO requestDTO) throws Exception;
}

