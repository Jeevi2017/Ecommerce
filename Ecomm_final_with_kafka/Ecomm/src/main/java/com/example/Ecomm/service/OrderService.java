package com.example.Ecomm.service;

import java.util.List;
import com.example.Ecomm.dto.OrderDTO;

public interface OrderService {

    // ✅ ONE-STEP CHECKOUT (address REQUIRED)
    OrderDTO placeOrder(Long customerId, Long addressId);

    List<OrderDTO> getAllOrders();

    OrderDTO getOrderById(Long orderId);

    void deleteOrder(Long orderId);

    List<OrderDTO> getOrdersByCustomerId(Long customerId);

    Long getCustomerIdForOrderInternal(Long orderId);

    OrderDTO updateOrderStatus(Long orderId, String status);

    void cancelOrder(Long orderId);

    OrderDTO updateOrderFull(Long orderId, OrderDTO updatedDetails);
}
