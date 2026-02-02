package com.example.Ecomm.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecomm.dto.OrderDTO;
import com.example.Ecomm.dto.OrderItemDTO;
import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.entitiy.*;
import com.example.Ecomm.entitiy.Order.OrderStatus;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.*;
import com.example.Ecomm.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private AddressRepository addressRepository;

    // ================= PLACE ORDER =================
    // Order is created, but CART IS NOT CLEARED HERE

    @Override
    @Transactional
    public OrderDTO placeOrder(Long customerId, Long addressId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (address.getProfile() == null ||
                address.getProfile().getCustomer() == null ||
                !address.getProfile().getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Address does not belong to this customer");
        }

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with empty cart");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(address);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(cart.getTotalAmount());
        order.setCouponCode(cart.getCouponCode());
        order.setDiscountAmount(cart.getDiscountAmount());

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProduct().getId()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());

            order.addOrderItem(orderItem);
        }

        // ✅ DO NOT CLEAR CART HERE
        return mapOrderToDTO(orderRepository.save(order));
    }

    // ================= FETCH =================

    @Override
    public OrderDTO getOrderById(Long id) {
        return mapOrderToDTO(
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Order", "id", id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) {

        List<Order> orders = orderRepository.findByCustomer_Id(customerId);

        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        return orders.stream()
                .map(order -> {
                    try {
                        return mapOrderToDTO(order);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapOrderToDTO)
                .collect(Collectors.toList());
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        return mapOrderToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO updateOrderFull(Long orderId, OrderDTO updatedDetails) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(OrderStatus.valueOf(updatedDetails.getStatus().toUpperCase()));
        return mapOrderToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId));
        orderRepository.delete(order);
    }

    @Override
    public Long getCustomerIdForOrderInternal(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId))
                .getCustomer()
                .getId();
    }

    // ================= DTO MAPPERS =================

    private OrderDTO mapOrderToDTO(Order order) {

        OrderDTO dto = new OrderDTO();

        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setCouponCode(order.getCouponCode());
        dto.setDiscountAmount(order.getDiscountAmount());

        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getId());
        }

        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(formatAddress(order.getShippingAddress()));
        }

        if (order.getOrderItems() != null) {
            dto.setOrderItems(
                    order.getOrderItems()
                            .stream()
                            .map(this::mapOrderItemToDTO)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setOrderItems(List.of());
        }

        return dto;
    }

    private OrderItemDTO mapOrderItemToDTO(OrderItem item) {

        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());

        if (item.getProduct() != null) {
            ProductDTO p = new ProductDTO();
            p.setId(item.getProduct().getId());
            p.setName(item.getProduct().getName());
            p.setPrice(item.getProduct().getPrice());
            p.setImages(item.getProduct().getImages());
            p.setDescription(item.getProduct().getDescription());
            p.setStockQuantity(item.getProduct().getStockQuantity());
            dto.setProductDetails(p);
        }

        return dto;
    }

    private String formatAddress(Address a) {
        return a.getStreet() + ", " + a.getCity() + ", "
                + a.getState() + " - " + a.getPostalCode();
    }
}
