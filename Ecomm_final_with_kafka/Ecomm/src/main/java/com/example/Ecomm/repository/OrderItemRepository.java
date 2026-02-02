package com.example.Ecomm.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.example.Ecomm.entitiy.OrderItem;

@Repository

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}
