package com.example.Ecomm.repository;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Ecomm.entitiy.Category;
import com.example.Ecomm.entitiy.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> { 
	Optional<Product> findById(Long id);

	void deleteById(Long productId);
	List<Product> findByCategory(Category category);

	Optional<Product> findByNameIgnoreCaseAndCategoryId(String name, Long categoryId);

    Optional<Product> findByName(String name);


}
