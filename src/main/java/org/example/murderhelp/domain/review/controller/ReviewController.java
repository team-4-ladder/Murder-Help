package org.example.murderhelp.domain.review.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.review.dto.*;
import org.example.murderhelp.domain.review.service.ReviewService;
import org.example.murderhelp.global.response.ApiResponse;
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
    public ApiResponse<List<PendingReviewResponse>> getPendingReviews(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(
                reviewService.getPendingReviews(memberId)
        );
    }

    /**
     * 로그인 회원이 작성한 리뷰 조회
     */
    @GetMapping("/me")
    public ApiResponse<List<MyReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(
                reviewService.getMyReviews(memberId)
        );
    }

    /**
     * 리뷰 작성
     */
    @PostMapping
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid ReviewCreateRequest request
    ) {
        return ApiResponse.ok(
                reviewService.createReview(memberId, request)
        );
    }

    /**
     * 리뷰 수정
     */
    @PatchMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewUpdateRequest request
    ) {
        return ApiResponse.ok(
                reviewService.updateReview(memberId, reviewId, request)
        );
    }

    /**
     * 리뷰 삭제
     */
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(memberId, reviewId);

        return ApiResponse.ok();
    }
}