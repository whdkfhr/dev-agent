package com.arok2.dev_agent.review.service;

import com.arok2.dev_agent.common.exception.ReviewAlreadyExistsException;
import com.arok2.dev_agent.common.exception.ReviewNotFoundException;
import com.arok2.dev_agent.review.domain.Review;
import com.arok2.dev_agent.review.dto.ReviewCreateRequest;
import com.arok2.dev_agent.review.dto.ReviewResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReviewService {

    private final Map<String, Review> reviewById = new ConcurrentHashMap<>();
    private final Map<String, Review> reviewByImplId = new ConcurrentHashMap<>();

    public ReviewResponse createReview(ReviewCreateRequest request) {
        if (reviewById.containsKey(request.reviewId())) {
            throw new ReviewAlreadyExistsException(request.reviewId());
        }
        Review review = new Review(
                request.reviewId(),
                request.implId(),
                request.taskId(),
                request.status(),
                request.comments(),
                Instant.now()
        );
        reviewById.put(review.reviewId(), review);
        reviewByImplId.put(review.implId(), review);
        return toResponse(review);
    }

    public List<ReviewResponse> getAllReviews() {
        return reviewById.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public ReviewResponse getReview(String reviewId) {
        Review review = reviewById.get(reviewId);
        if (review == null) {
            throw new ReviewNotFoundException("Review not found: " + reviewId);
        }
        return toResponse(review);
    }

    public ReviewResponse getReviewByImplId(String implId) {
        Review review = reviewByImplId.get(implId);
        if (review == null) {
            throw new ReviewNotFoundException("Review not found for implementation: " + implId);
        }
        return toResponse(review);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.reviewId(),
                review.implId(),
                review.taskId(),
                review.status(),
                review.comments(),
                review.createdAt()
        );
    }
}
