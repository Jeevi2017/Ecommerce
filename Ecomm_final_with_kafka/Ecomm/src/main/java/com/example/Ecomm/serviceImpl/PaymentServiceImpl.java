package com.example.Ecomm.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;

import org.json.JSONObject;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecomm.dto.PaymentDTO;
import com.example.Ecomm.dto.RazorpayOrderRequestDTO;
import com.example.Ecomm.dto.RazorpayOrderResponseDTO;
import com.example.Ecomm.dto.RazorpayPaymentCaptureRequestDTO;

import com.example.Ecomm.entitiy.Order;
import com.example.Ecomm.entitiy.Order.OrderStatus;
import com.example.Ecomm.entitiy.Payment;

import com.example.Ecomm.exception.ResourceNotFoundException;

import com.example.Ecomm.repository.OrderRepository;
import com.example.Ecomm.repository.PaymentRepository;

import com.example.Ecomm.service.CartService;
import com.example.Ecomm.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    // ✅ ADD THIS
    @Autowired
    private CartService cartService;

    // ================= NORMAL PAYMENT =================

    @Override
    @Transactional
    public PaymentDTO processPayment(Long orderId, PaymentDTO paymentDTO) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId));

        if (order.getShippingAddress() == null || order.getStatus() == OrderStatus.DRAFT) {
            throw new IllegalStateException(
                    "Please select shipping address before proceeding to payment"
            );
        }

        if (order.getStatus() == OrderStatus.PAID ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order already PAID or CANCELLED");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED");

        Payment savedPayment = paymentRepository.save(payment);

        // ✅ PAYMENT SUCCESS
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // ✅ CLEAR CART ONLY AFTER PAYMENT SUCCESS
        cartService.clearCart(order.getCustomer().getId());

        return convertToDTO(savedPayment);
    }

    // ================= RAZORPAY ORDER =================

    @Override
    public RazorpayOrderResponseDTO createRazorpayOrder(RazorpayOrderRequestDTO requestDTO) throws Exception {

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        BigDecimal amountInRupees = BigDecimal.valueOf(requestDTO.getAmount());
        long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", requestDTO.getCurrency());
        orderRequest.put("receipt", requestDTO.getReceipt());

        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

        RazorpayOrderResponseDTO dto = new RazorpayOrderResponseDTO();
        dto.setId((String) razorpayOrder.get("id"));
        dto.setEntity((String) razorpayOrder.get("entity"));
        dto.setAmount(((Number) razorpayOrder.get("amount")).longValue());
        dto.setAttempts(((Number) razorpayOrder.get("attempts")).intValue());

        Object createdAt = razorpayOrder.get("created_at");
        if (createdAt instanceof Number) {
            dto.setCreatedAt(((Number) createdAt).longValue());
        } else if (createdAt instanceof Date) {
            dto.setCreatedAt(((Date) createdAt).getTime() / 1000);
        }

        dto.setCurrency((String) razorpayOrder.get("currency"));
        dto.setReceipt((String) razorpayOrder.get("receipt"));
        dto.setStatus((String) razorpayOrder.get("status"));

        return dto;
    }

    // ================= RAZORPAY CAPTURE =================

    @Override
    @Transactional
    public PaymentDTO captureRazorpayPayment(RazorpayPaymentCaptureRequestDTO requestDTO) throws Exception {

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", requestDTO.getRazorpayOrderId());
        options.put("razorpay_payment_id", requestDTO.getRazorpayPaymentId());
        options.put("razorpay_signature", requestDTO.getRazorpaySignature());

        if (!Utils.verifyPaymentSignature(options, razorpayKeySecret)) {
            throw new IllegalArgumentException("Invalid Razorpay signature");
        }

        Order order = orderRepository.findById(requestDTO.getInternalOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", requestDTO.getInternalOrderId()));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("RAZORPAY");
        payment.setAmount(requestDTO.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED");

        Payment savedPayment = paymentRepository.save(payment);

        // ✅ PAYMENT SUCCESS
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // ✅ CLEAR CART ONLY AFTER PAYMENT SUCCESS
        cartService.clearCart(order.getCustomer().getId());

        return convertToDTO(savedPayment);
    }

    // ================= WEBHOOK PAYMENT =================

    @Override
    @Transactional
    public PaymentDTO captureWebhookPayment(JSONObject paymentEntity, Long internalOrderId) {

        Order order = orderRepository.findById(internalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", internalOrderId));

        BigDecimal amountInRupees = BigDecimal
                .valueOf(paymentEntity.getLong("amount"))
                .divide(BigDecimal.valueOf(100));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("RAZORPAY");
        payment.setAmount(amountInRupees);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED");

        Payment saved = paymentRepository.save(payment);

        // ✅ PAYMENT SUCCESS
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // ✅ CLEAR CART ONLY AFTER PAYMENT SUCCESS
        cartService.clearCart(order.getCustomer().getId());

        return convertToDTO(saved);
    }

    // ================= QUERY METHODS =================

    @Override
    public PaymentDTO getPaymentById(Long paymentId) {
        return convertToDTO(
                paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", paymentId))
        );
    }

    @Override
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll()
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PaymentDTO> getPaymentsByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId));

        return paymentRepository.findByOrder(order)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", paymentId));
        paymentRepository.delete(payment);
    }

    // ================= HELPERS =================

    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder().getId());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setStatus(payment.getStatus());
        return dto;
    }
}
