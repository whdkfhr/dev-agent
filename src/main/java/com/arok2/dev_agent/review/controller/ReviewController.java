package com.arok2.dev_agent.review.controller;

import com.arok2.dev_agent.review.dto.ReviewCreateRequest;
import com.arok2.dev_agent.review.dto.ReviewResponse;
import com.arok2.dev_agent.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.getReview(reviewId));
    }

    @GetMapping("/by-impl/{implId}")
    public ResponseEntity<ReviewResponse> getReviewByImplId(@PathVariable String implId) {
        return ResponseEntity.ok(reviewService.getReviewByImplId(implId));
    }
}
