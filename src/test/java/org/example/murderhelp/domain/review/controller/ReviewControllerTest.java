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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long REVIEW_ID = 100L;
    private static final Long ORDER_ITEM_ID = 10L;
    private static final Long PRODUCT_ID = 1L;

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 가능 상품 조회 요청을 서비스에 전달한다")
    void getPendingReviews() {
        // given
        LocalDateTime purchasedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        15,
                        15,
                        30
                );

        PendingReviewResponse pendingReview =
                new PendingReviewResponse(
                        ORDER_ITEM_ID,
                        "P001",
                        "리뷰 테스트 상품",
                        purchasedAt,
                        "https://example.com/product.png"
                );

        when(reviewService.getPendingReviews(MEMBER_ID))
                .thenReturn(
                        List.of(pendingReview)
                );

        // when
        ResponseEntity<ApiResponse<List<PendingReviewResponse>>> result =
                reviewController.getPendingReviews(
                        MEMBER_ID
                );

        // then
        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(result.getBody())
                .isNotNull();

        ApiResponse<List<PendingReviewResponse>> body =
                result.getBody();

        assertThat(body.getCode())
                .isEqualTo("SUCCESS");

        assertThat(body.getData())
                .containsExactly(pendingReview);

        verify(reviewService)
                .getPendingReviews(MEMBER_ID);
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 요청을 서비스에 전달한다")
    void getMyReviews() {
        // given
        MyReviewResponse review =
                myReviewResponse(
                        REVIEW_ID,
                        ORDER_ITEM_ID,
                        PRODUCT_ID,
                        "P001",
                        "리뷰 테스트 상품",
                        5,
                        "좋은 상품입니다."
                );

        when(reviewService.getMyReviews(MEMBER_ID))
                .thenReturn(
                        List.of(review)
                );

        // when
        ResponseEntity<ApiResponse<List<MyReviewResponse>>> result =
                reviewController.getMyReviews(
                        MEMBER_ID
                );

        // then
        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(result.getBody())
                .isNotNull();

        ApiResponse<List<MyReviewResponse>> body =
                result.getBody();

        assertThat(body.getCode())
                .isEqualTo("SUCCESS");

        assertThat(body.getData())
                .containsExactly(review);

        assertThat(body.getData().get(0).productCode())
                .isEqualTo("P001");

        assertThat(body.getData().get(0).productName())
                .isEqualTo("리뷰 테스트 상품");

        verify(reviewService)
                .getMyReviews(MEMBER_ID);
    }

    @Test
    @DisplayName("리뷰 작성 요청을 서비스에 전달한다")
    void createReview() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        ORDER_ITEM_ID,
                        5,
                        "리뷰 작성 테스트"
                );

        ReviewResponse response =
                reviewResponse(
                        REVIEW_ID,
                        ORDER_ITEM_ID,
                        PRODUCT_ID,
                        5,
                        "리뷰 작성 테스트"
                );

        when(
                reviewService.createReview(
                        MEMBER_ID,
                        request
                )
        ).thenReturn(response);

        // when
        ResponseEntity<ApiResponse<ReviewResponse>> result =
                reviewController.createReview(
                        MEMBER_ID,
                        request
                );

        // then
        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(result.getBody())
                .isNotNull();

        ApiResponse<ReviewResponse> body =
                result.getBody();

        assertThat(body.getCode())
                .isEqualTo("SUCCESS");

        assertThat(body.getData())
                .isEqualTo(response);

        verify(reviewService)
                .createReview(
                        MEMBER_ID,
                        request
                );
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
                        REVIEW_ID,
                        ORDER_ITEM_ID,
                        PRODUCT_ID,
                        4,
                        "수정된 리뷰입니다."
                );

        when(
                reviewService.updateReview(
                        MEMBER_ID,
                        REVIEW_ID,
                        request
                )
        ).thenReturn(response);

        // when
        ResponseEntity<ApiResponse<ReviewResponse>> result =
                reviewController.updateReview(
                        MEMBER_ID,
                        REVIEW_ID,
                        request
                );

        // then
        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(result.getBody())
                .isNotNull();

        ApiResponse<ReviewResponse> body =
                result.getBody();

        assertThat(body.getCode())
                .isEqualTo("SUCCESS");

        assertThat(body.getData())
                .isEqualTo(response);

        verify(reviewService)
                .updateReview(
                        MEMBER_ID,
                        REVIEW_ID,
                        request
                );
    }

    @Test
    @DisplayName("리뷰 삭제 요청을 서비스에 전달한다")
    void deleteReview() {
        // when
        ResponseEntity<ApiResponse<Void>> result =
                reviewController.deleteReview(
                        MEMBER_ID,
                        REVIEW_ID
                );

        // then
        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(result.getBody())
                .isNotNull();

        ApiResponse<Void> body =
                result.getBody();

        assertThat(body.getCode())
                .isEqualTo("SUCCESS");

        assertThat(body.getData())
                .isNull();

        verify(reviewService)
                .deleteReview(
                        MEMBER_ID,
                        REVIEW_ID
                );
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
        LocalDateTime now =
                LocalDateTime.now();

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
        LocalDateTime now =
                LocalDateTime.now();

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