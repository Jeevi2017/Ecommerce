package com.example.Ecomm.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;


@Service
public class OrderServiceImpl implements OrderService {

    private static final String ENTITY_ORDER = "Order";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;

    // ✅ Constructor Injection (Sonar compliant)
    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            ProductRepository productRepository,
                            CartRepository cartRepository,
                            AddressRepository addressRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
    }

    // ================= PLACE ORDER =================

    @Override
    @Transactional
    public OrderDTO placeOrder(Long customerId, Long addressId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer", "id", customerId));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address", "id", addressId));

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
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Product", "id", cartItem.getProduct().getId()));

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

        return mapOrderToDTO(orderRepository.save(order));
    }

    // ================= FETCH =================

    @Override
    public OrderDTO getOrderById(Long id) {
        return mapOrderToDTO(
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(ENTITY_ORDER, "id", id))
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
                .toList(); // ✅ Sonar compliant
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapOrderToDTO)
                .toList(); // ✅ Sonar compliant
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_ORDER, "id", orderId));

        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        return mapOrderToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO updateOrderFull(Long orderId, OrderDTO updatedDetails) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_ORDER, "id", orderId));

        // current order status
        OrderStatus currentStatus = order.getStatus();

        // new status from UI
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(updatedDetails.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order status"
            );
        }

        // 🚫 IMPORTANT BUSINESS RULE
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order Status cannot be updated After Delivered Or Cancelled"
            );
        }

        // update allowed
        order.setStatus(newStatus);

        return mapOrderToDTO(orderRepository.save(order));
    }


    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_ORDER, "id", orderId));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_ORDER, "id", orderId));
        orderRepository.delete(order);
    }

    @Override
    public Long getCustomerIdForOrderInternal(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_ORDER, "id", orderId))
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

        dto.setOrderItems(
                order.getOrderItems() != null
                        ? order.getOrderItems().stream().map(this::mapOrderItemToDTO).toList()
                        : List.of()
        );

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
