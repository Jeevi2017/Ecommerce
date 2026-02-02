package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // NEW: Import PreAuthorize
import org.springframework.security.core.Authentication; // NEW: Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // NEW: Import SecurityContextHolder
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecomm.config.SecurityConstants; // NEW: Import SecurityConstants
import com.example.Ecomm.dto.OrderDTO; // NEW: Import OrderDTO for helper method
import com.example.Ecomm.dto.OrderItemDTO;
import com.example.Ecomm.dto.UserDTO; // NEW: Import UserDTO for helper method
import com.example.Ecomm.exception.ResourceNotFoundException; // NEW: Import ResourceNotFoundException
import com.example.Ecomm.service.OrderItemService;
import com.example.Ecomm.service.OrderService; // NEW: Autowire OrderService
import com.example.Ecomm.service.UserService; // NEW: Autowire UserService

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderItemController {

	@Autowired
	private OrderItemService orderItemService;

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

	public Long getOrderItemOwnerId(Long orderItemId) {
		OrderItemDTO orderItemDTO = orderItemService.getOrderItemById(orderItemId);
		if (orderItemDTO != null && orderItemDTO.getId() != null) {
			OrderDTO orderDTO = orderService.getOrderById(orderItemDTO.getId());
			if (orderDTO != null && orderDTO.getCustomerId() != null) {
				return orderDTO.getCustomerId();
			}
		}
		throw new ResourceNotFoundException("OrderItem or its associated Order", "Id for owner check", orderItemId);
	}


	@PostMapping
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
	public ResponseEntity<OrderItemDTO> createOrderItem(@Validated @RequestBody OrderItemDTO orderItemDTO) {
		OrderItemDTO savedOrderItem = orderItemService.saveOrderItem(orderItemDTO);
		return new ResponseEntity<>(savedOrderItem, HttpStatus.CREATED);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
	public ResponseEntity<List<OrderItemDTO>> getAllOrderItems() {
		List<OrderItemDTO> orderItems = orderItemService.getAllOrderItems();
		return new ResponseEntity<>(orderItems, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or @orderItemController.getAuthenticatedCustomerId() == @orderItemController.getOrderItemOwnerId(#id)")
	public ResponseEntity<OrderItemDTO> getOrderItemById(@PathVariable Long id) {
		OrderItemDTO orderItem = orderItemService.getOrderItemById(id);
		return new ResponseEntity<>(orderItem, HttpStatus.OK);
	}
}
