package com.example.Ecomm.service;

import java.util.List;
import com.example.Ecomm.dto.OrderItemDTO;

public interface OrderItemService {

    OrderItemDTO saveOrderItem(OrderItemDTO orderItemDTO);

    List<OrderItemDTO> getAllOrderItems();

    OrderItemDTO getOrderItemById(Long id);
}
