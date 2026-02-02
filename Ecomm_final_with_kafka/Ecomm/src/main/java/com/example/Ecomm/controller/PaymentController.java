package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecomm.config.SecurityConstants; // NEW: Import SecurityConstants
import com.example.Ecomm.dto.CustomerDTO; // Still needed for customerService.getCustomerByUsername return type (UserDTO)
import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.dto.PaymentDTO;
import com.example.Ecomm.dto.RazorpayOrderRequestDTO;
import com.example.Ecomm.dto.RazorpayOrderResponseDTO;
import com.example.Ecomm.dto.RazorpayPaymentCaptureRequestDTO;
import com.example.Ecomm.dto.UserDTO; // NEW: Import UserDTO for getAuthenticatedCustomerId
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.OrderService;
import com.example.Ecomm.service.PaymentService;
import com.example.Ecomm.service.UserService; // NEW: Import UserService for general user lookup

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins ="http://localhost:4200")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private OrderService orderService;

    @Autowired
    private UserService userService; 


	public Long getAuthenticatedCustomerId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String authenticatedUsername = authentication.getName(); 
		UserDTO userDTO = userService.getUserByUserName(authenticatedUsername); 
		return userDTO.getId(); 
	}

	public Long getOrderOwnerId(Long orderId) {
		OrderDTO orderDTO = orderService.getOrderById(orderId);
		if (orderDTO != null) {
			return orderDTO.getCustomerId();
		}
		throw new ResourceNotFoundException("Order", "Id for owner check", orderId);
	}

	public Long getPaymentOwnerId(Long paymentId) {
		PaymentDTO paymentDTO = paymentService.getPaymentById(paymentId);
		if (paymentDTO != null && paymentDTO.getOrderId() != null) {
			return getOrderOwnerId(paymentDTO.getOrderId());
		}
		throw new ResourceNotFoundException("Payment or its associated Order", "Id for owner check", paymentId);
	}

	@PostMapping("/order/{orderId}")
	@PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')") // Using constants
	public ResponseEntity<PaymentDTO> processPayment(@PathVariable Long orderId,
			@Validated @RequestBody PaymentDTO paymentDTO) {
		PaymentDTO savedPayment = paymentService.processPayment(orderId, paymentDTO);
		return new ResponseEntity<>(savedPayment, HttpStatus.CREATED);
	}

	@PostMapping("/razorpay/order")
	@PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')") // Using constants
	public ResponseEntity<RazorpayOrderResponseDTO> createRazorpayOrder(@Validated @RequestBody RazorpayOrderRequestDTO requestDTO) {
		try {
			RazorpayOrderResponseDTO response = paymentService.createRazorpayOrder(requestDTO);
			return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); // 👈 THIS IS CRITICAL
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

	@PostMapping("/razorpay/capture")
	@PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')") // Using constants
	public ResponseEntity<PaymentDTO> captureRazorpayPayment(@Validated @RequestBody RazorpayPaymentCaptureRequestDTO requestDTO) {
		try {
			PaymentDTO paymentDTO = paymentService.captureRazorpayPayment(requestDTO);
			return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
		} catch (Exception e) {
			System.err.println("Error capturing Razorpay payment: " + e.getMessage());
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/{paymentId}")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or #paymentId == @paymentController.getPaymentOwnerId(#paymentId)") // Using constant
	public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long paymentId) {
		PaymentDTO paymentDTO = paymentService.getPaymentById(paymentId);
		return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')") 
	public ResponseEntity<List<PaymentDTO>> getAllPayments() {
		List<PaymentDTO> payments = paymentService.getAllPayments();
		return new ResponseEntity<>(payments, HttpStatus.OK);
	}

	@GetMapping("/order/{orderId}")
	@PreAuthorize("hasAnyAuthority('" + SecurityConstants.ROLE_ADMIN + "', '" + SecurityConstants.ROLE_CUSTOMER + "')") // Using constants
	public ResponseEntity<List<PaymentDTO>> getPaymentsByOrderId(@PathVariable Long orderId) {
		List<PaymentDTO> payments = paymentService.getPaymentsByOrderId(orderId);
		return new ResponseEntity<>(payments, HttpStatus.OK);
	}

	@DeleteMapping("/{paymentId}")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')") 
	public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
		paymentService.deletePayment(paymentId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
