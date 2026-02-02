package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecomm.entitiy.Review;
import com.example.Ecomm.service.ReviewService;

@RestController
@RequestMapping("api/reviews")
@CrossOrigin(origins="http://localhost:4200")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_CUSTOMER')" )
    public ResponseEntity<Review> addReview(@RequestBody Review review){
        Review savedReview = reviewService.addReview(review);
        return ResponseEntity.ok(savedReview);
    }
    @GetMapping("/products/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Review>> getReviewsForProduct(@PathVariable Long productId){
        List<Review> reviews = reviewService.getReviewsForProduct(productId);

        return ResponseEntity.ok(reviews);
    }
}