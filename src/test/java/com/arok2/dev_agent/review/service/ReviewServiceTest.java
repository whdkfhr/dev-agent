package com.arok2.dev_agent.review.service;

import com.arok2.dev_agent.common.exception.ReviewAlreadyExistsException;
import com.arok2.dev_agent.common.exception.ReviewNotFoundException;
import com.arok2.dev_agent.review.dto.ReviewCreateRequest;
import com.arok2.dev_agent.review.dto.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewServiceTest {

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService();
    }

    @Test
    void createReview_저장_후_응답_반환() {
        ReviewCreateRequest request = new ReviewCreateRequest(
                "REVIEW-001", "IMPL-001", "TASK-001", "APPROVED",
                List.of("코드 구조가 명확합니다.")
        );

        ReviewResponse response = reviewService.createReview(request);

        assertThat(response.reviewId()).isEqualTo("REVIEW-001");
        assertThat(response.implId()).isEqualTo("IMPL-001");
        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.comments()).containsExactly("코드 구조가 명확합니다.");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createReview_중복_reviewId_예외() {
        ReviewCreateRequest request = new ReviewCreateRequest(
                "REVIEW-001", "IMPL-001", "TASK-001", "APPROVED", List.of()
        );
        reviewService.createReview(request);

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(ReviewAlreadyExistsException.class)
                .hasMessageContaining("REVIEW-001");
    }

    @Test
    void getAllReviews_전체_목록_반환() {
        reviewService.createReview(new ReviewCreateRequest(
                "REVIEW-001", "IMPL-001", "TASK-001", "APPROVED", List.of()
        ));
        reviewService.createReview(new ReviewCreateRequest(
                "REVIEW-002", "IMPL-002", "TASK-002", "REJECTED", List.of()
        ));

        List<ReviewResponse> reviews = reviewService.getAllReviews();

        assertThat(reviews).hasSize(2);
    }

    @Test
    void getReview_존재하는_reviewId_반환() {
        reviewService.createReview(new ReviewCreateRequest(
                "REVIEW-001", "IMPL-001", "TASK-001", "APPROVED", List.of()
        ));

        ReviewResponse response = reviewService.getReview("REVIEW-001");

        assertThat(response.reviewId()).isEqualTo("REVIEW-001");
    }

    @Test
    void getReview_존재하지않는_reviewId_예외() {
        assertThatThrownBy(() -> reviewService.getReview("REVIEW-999"))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("REVIEW-999");
    }

    @Test
    void getReviewByImplId_연관_Review_반환() {
        reviewService.createReview(new ReviewCreateRequest(
                "REVIEW-001", "IMPL-001", "TASK-001", "APPROVED", List.of()
        ));

        ReviewResponse response = reviewService.getReviewByImplId("IMPL-001");

        assertThat(response.implId()).isEqualTo("IMPL-001");
        assertThat(response.reviewId()).isEqualTo("REVIEW-001");
    }

    @Test
    void getReviewByImplId_존재하지않는_implId_예외() {
        assertThatThrownBy(() -> reviewService.getReviewByImplId("IMPL-999"))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("IMPL-999");
    }
}
