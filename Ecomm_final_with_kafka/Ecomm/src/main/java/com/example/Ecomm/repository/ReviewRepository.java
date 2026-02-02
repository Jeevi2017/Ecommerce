package com.example.Ecomm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Ecomm.entitiy.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>{

    List<Review> findByProductId(Long productId);
}