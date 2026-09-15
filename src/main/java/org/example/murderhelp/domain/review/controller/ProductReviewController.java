package org.example.murderhelp.domain.review.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.service.ReviewService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ReviewService reviewService;

    /**
     * 특정 상품에 등록된 리뷰를 최신순으로 조회한다.
     */
    @GetMapping
    public ApiResponse<List<ReviewResponse>> getProductReviews(
            @PathVariable Long productId
    ) {
        return ApiResponse.ok(
                reviewService.getProductReviews(productId)
        );
    }
}