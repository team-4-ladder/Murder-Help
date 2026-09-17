package org.example.murderhelp.domain.review.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.review.dto.MyReviewResponse;
import org.example.murderhelp.domain.review.dto.PendingReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewCreateRequest;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewUpdateRequest;
import org.example.murderhelp.domain.review.service.ReviewService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성 가능한 주문상품 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingReviewResponse>>> getPendingReviews(
            @AuthenticationPrincipal Long memberId
    ) {
        List<PendingReviewResponse> response =
                reviewService.getPendingReviews(memberId);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * 로그인 회원이 작성한 리뷰 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MyReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal Long memberId
    ) {
        List<MyReviewResponse> response =
                reviewService.getMyReviews(memberId);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * 리뷰 작성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewResponse response =
                reviewService.createReview(
                        memberId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * 리뷰 수정
     */
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        ReviewResponse response =
                reviewService.updateReview(
                        memberId,
                        reviewId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * 리뷰 삭제
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(
                memberId,
                reviewId
        );

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }
}