package com.example.Ecomm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Ecomm.entitiy.Order;
import com.example.Ecomm.entitiy.Order.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer_Id(Long customerId);

    long countByCustomer_Id(Long customerId);

    // ✅ ADD THIS (VERY IMPORTANT)
    Optional<Order> findTopByCustomer_IdAndStatusOrderByOrderDateDesc(
            Long customerId,
            OrderStatus status
    );
}
