package org.example.murderhelp.domain.review.dto;

import java.time.LocalDateTime;

public record MyReviewResponse(
        Long reviewId,
        Long orderItemId,
        Long productId,
        String productCode,
        String productName,
        Integer rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}