package com.example.Ecomm.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Ecomm.entitiy.Order;
import com.example.Ecomm.entitiy.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	List<Payment> findByOrder(Order order);

}
