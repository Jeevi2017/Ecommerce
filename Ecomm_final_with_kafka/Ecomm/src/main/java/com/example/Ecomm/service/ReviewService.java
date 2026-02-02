package com.example.Ecomm.service;

import java.util.List;

import com.example.Ecomm.entitiy.Review;


public interface ReviewService {
	Review addReview(Review review);
	List<Review> getReviewsForProduct(Long productId);
	
}
