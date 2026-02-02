package com.example.Ecomm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Ecomm.entitiy.Cart;
import com.example.Ecomm.entitiy.Customer;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
	Optional<Cart> findByCustomer(Customer customer);


    Optional<Cart> findByCustomerId(Long customerId);


}
