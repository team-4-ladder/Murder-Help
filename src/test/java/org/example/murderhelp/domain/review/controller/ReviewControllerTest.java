package org.example.murderhelp.domain.review.controller;

import org.example.murderhelp.domain.review.dto.MyReviewResponse;
import org.example.murderhelp.domain.review.dto.PendingReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewCreateRequest;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewUpdateRequest;
import org.example.murderhelp.domain.review.service.ReviewService;
import org.example.murderhelp.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 가능 상품 조회 요청을 서비스에 전달한다")
    void getPendingReviews() {
        // given
        LocalDateTime purchasedAt =
                LocalDateTime.of(2026, 9, 15, 15, 30);

        PendingReviewResponse pendingReview =
                new PendingReviewResponse(
                        10L,
                        "P001",
                        "리뷰 테스트 상품",
                        purchasedAt,
                        "https://example.com/product.png"
                );

        when(reviewService.getPendingReviews(1L))
                .thenReturn(List.of(pendingReview));

        // when
        ApiResponse<List<PendingReviewResponse>> result =
                reviewController.getPendingReviews(1L);

        // then
        assertThat(result.getCode())
                .isEqualTo("SUCCESS");

        assertThat(result.getData())
                .containsExactly(pendingReview);

        verify(reviewService)
                .getPendingReviews(1L);
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 요청을 서비스에 전달한다")
    void getMyReviews() {
        // given
        MyReviewResponse review =
                myReviewResponse(
                        100L,
                        10L,
                        1L,
                        "P001",
                        "리뷰 테스트 상품",
                        5,
                        "좋은 상품입니다."
                );

        when(reviewService.getMyReviews(1L))
                .thenReturn(List.of(review));

        // when
        ApiResponse<List<MyReviewResponse>> result =
                reviewController.getMyReviews(1L);

        // then
        assertThat(result.getCode())
                .isEqualTo("SUCCESS");

        assertThat(result.getData())
                .containsExactly(review);

        assertThat(result.getData().get(0).productCode())
                .isEqualTo("P001");

        assertThat(result.getData().get(0).productName())
                .isEqualTo("리뷰 테스트 상품");

        verify(reviewService)
                .getMyReviews(1L);
    }

    @Test
    @DisplayName("리뷰 작성 요청을 서비스에 전달한다")
    void createReview() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        10L,
                        5,
                        "리뷰 작성 테스트"
                );

        ReviewResponse response =
                reviewResponse(
                        100L,
                        10L,
                        1L,
                        5,
                        "리뷰 작성 테스트"
                );

        when(reviewService.createReview(1L, request))
                .thenReturn(response);

        // when
        ApiResponse<ReviewResponse> result =
                reviewController.createReview(1L, request);

        // then
        assertThat(result.getCode())
                .isEqualTo("SUCCESS");

        assertThat(result.getData())
                .isEqualTo(response);

        verify(reviewService)
                .createReview(1L, request);
    }

    @Test
    @DisplayName("리뷰 수정 요청을 서비스에 전달한다")
    void updateReview() {
        // given
        ReviewUpdateRequest request =
                new ReviewUpdateRequest(
                        4,
                        "수정된 리뷰입니다."
                );

        ReviewResponse response =
                reviewResponse(
                        100L,
                        10L,
                        1L,
                        4,
                        "수정된 리뷰입니다."
                );

        when(reviewService.updateReview(1L, 100L, request))
                .thenReturn(response);

        // when
        ApiResponse<ReviewResponse> result =
                reviewController.updateReview(
                        1L,
                        100L,
                        request
                );

        // then
        assertThat(result.getCode())
                .isEqualTo("SUCCESS");

        assertThat(result.getData())
                .isEqualTo(response);

        verify(reviewService)
                .updateReview(1L, 100L, request);
    }

    @Test
    @DisplayName("리뷰 삭제 요청을 서비스에 전달한다")
    void deleteReview() {
        // when
        ApiResponse<Void> result =
                reviewController.deleteReview(1L, 100L);

        // then
        assertThat(result.getCode())
                .isEqualTo("SUCCESS");

        verify(reviewService)
                .deleteReview(1L, 100L);
    }

    private MyReviewResponse myReviewResponse(
            Long reviewId,
            Long orderItemId,
            Long productId,
            String productCode,
            String productName,
            Integer rating,
            String content
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new MyReviewResponse(
                reviewId,
                orderItemId,
                productId,
                productCode,
                productName,
                rating,
                content,
                now,
                now
        );
    }

    private ReviewResponse reviewResponse(
            Long reviewId,
            Long orderItemId,
            Long productId,
            Integer rating,
            String content
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new ReviewResponse(
                reviewId,
                orderItemId,
                productId,
                rating,
                content,
                now,
                now
        );
    }
}