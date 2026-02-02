package com.example.Ecomm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Ecomm.entitiy.Cart;
import com.example.Ecomm.entitiy.CartItem;
import com.example.Ecomm.entitiy.Product;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

	List<CartItem> findByProduct(Product product);
	void deleteByCartAndProduct(Cart cart, Product product);
}
