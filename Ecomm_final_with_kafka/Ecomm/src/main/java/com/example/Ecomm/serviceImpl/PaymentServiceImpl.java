package com.example.Ecomm.serviceImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

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

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            CartService cartService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    // ================= NORMAL PAYMENT =================

    @Override
    @Transactional
    public PaymentDTO processPayment(Long orderId, PaymentDTO paymentDTO) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId));

        if (order.getShippingAddress() == null || order.getStatus() == OrderStatus.DRAFT) {
            throw new IllegalStateException("Please select shipping address before proceeding to payment");
        }

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order already PAID or CANCELLED");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED");

        Payment savedPayment = paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        cartService.clearCart(order.getCustomer().getId());

        return convertToDTO(savedPayment);
    }

    // ================= RAZORPAY ORDER =================

    @Override
    public RazorpayOrderResponseDTO createRazorpayOrder(
            RazorpayOrderRequestDTO requestDTO
    ) throws JSONException, IOException {

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            long amountInPaise = BigDecimal.valueOf(requestDTO.getAmount())
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", requestDTO.getCurrency());
            orderRequest.put("receipt", requestDTO.getReceipt());

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            RazorpayOrderResponseDTO dto = new RazorpayOrderResponseDTO();
            dto.setId(razorpayOrder.get("id").toString());
            dto.setEntity(razorpayOrder.get("entity").toString());

            Object amount = razorpayOrder.get("amount");
            if (amount instanceof Number number) {
                dto.setAmount(number.longValue());
            }

            Object attempts = razorpayOrder.get("attempts");
            if (attempts instanceof Number number) {
                dto.setAttempts(number.intValue());
            }

            Object createdAt = razorpayOrder.get("created_at");
            if (createdAt instanceof Number number) {
                dto.setCreatedAt(number.longValue());
            } else if (createdAt instanceof Date date) {
                dto.setCreatedAt(date.getTime() / 1000);
            }

            dto.setCurrency(razorpayOrder.get("currency").toString());
            dto.setReceipt(razorpayOrder.get("receipt").toString());
            dto.setStatus(razorpayOrder.get("status").toString());

            return dto;

        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create Razorpay order", e);
        }
    }

    // ================= RAZORPAY CAPTURE =================

    @Override
    @Transactional
    public PaymentDTO captureRazorpayPayment(
            RazorpayPaymentCaptureRequestDTO requestDTO
    ) throws JSONException, IOException {

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", requestDTO.getRazorpayOrderId());
            options.put("razorpay_payment_id", requestDTO.getRazorpayPaymentId());
            options.put("razorpay_signature", requestDTO.getRazorpaySignature());

            if (!Utils.verifyPaymentSignature(options, razorpayKeySecret)) {
                throw new IllegalArgumentException("Invalid Razorpay signature");
            }

            Order order = orderRepository.findById(requestDTO.getInternalOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Order", "Id", requestDTO.getInternalOrderId()));

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMethod("RAZORPAY");
            payment.setAmount(requestDTO.getAmount());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus("COMPLETED");

            Payment savedPayment = paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            cartService.clearCart(order.getCustomer().getId());

            return convertToDTO(savedPayment);

        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to capture Razorpay payment", e);
        }
    }

    // ================= WEBHOOK =================

    @Override
    @Transactional
    public PaymentDTO captureWebhookPayment(JSONObject paymentEntity, Long internalOrderId) {

        Order order = orderRepository.findById(internalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", internalOrderId));

        BigDecimal amount = BigDecimal.valueOf(paymentEntity.getLong("amount"))
                .divide(BigDecimal.valueOf(100));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("RAZORPAY");
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED");

        Payment saved = paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        cartService.clearCart(order.getCustomer().getId());

        return convertToDTO(saved);
    }

    // ================= QUERIES =================

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
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<PaymentDTO> getPaymentsByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId));

        return paymentRepository.findByOrder(order)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", paymentId));
        paymentRepository.delete(payment);
    }

    // ================= MAPPER =================

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
