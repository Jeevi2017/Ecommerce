package com.example.Ecomm.controller;

import com.example.Ecomm.entitiy.Review;
import com.example.Ecomm.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private ReviewService reviewService;

    // ---------- HELPERS ----------

    private Review mockReview() {
        Review review = new Review();
        review.setId(1L);
        review.setRating(5);   // ✅ only fields that exist
        return review;
    }

    // ---------- TESTS ----------

    @Test
    void addReview() {
        Review review = mockReview();

        when(reviewService.addReview(any(Review.class)))
                .thenReturn(review);

        ResponseEntity<Review> response =
                reviewController.addReview(review);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());

        verify(reviewService, times(1))
                .addReview(review);
    }

    @Test
    void getReviewsForProduct() {
        when(reviewService.getReviewsForProduct(10L))
                .thenReturn(List.of(mockReview()));

        ResponseEntity<List<Review>> response =
                reviewController.getReviewsForProduct(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(reviewService, times(1))
                .getReviewsForProduct(10L);
    }
}
