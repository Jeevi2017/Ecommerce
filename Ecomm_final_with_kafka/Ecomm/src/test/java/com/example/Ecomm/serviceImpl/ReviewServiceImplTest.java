package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.entitiy.Review;
import com.example.Ecomm.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Mock
    private ReviewRepository reviewRepository;

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

        when(reviewRepository.save(review)).thenReturn(review);

        Review savedReview = reviewService.addReview(review);

        assertNotNull(savedReview);
        assertEquals(5, savedReview.getRating());
        verify(reviewRepository).save(review);
    }

    @Test
    void getReviewsForProduct() {
        Review review = mockReview();

        when(reviewRepository.findByProductId(1L))
                .thenReturn(List.of(review));

        List<Review> reviews = reviewService.getReviewsForProduct(1L);

        assertNotNull(reviews);
        assertEquals(1, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        verify(reviewRepository).findByProductId(1L);
    }
}
