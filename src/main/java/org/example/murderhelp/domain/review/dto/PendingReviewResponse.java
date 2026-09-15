package org.example.murderhelp.domain.review.dto;

import org.example.murderhelp.domain.order.entity.OrderItem;

import java.time.LocalDateTime;

public record PendingReviewResponse(
        Long orderItemId,
        String productCode,
        String productName,
        LocalDateTime purchasedAt,
        String imageUrl
) {

    public static PendingReviewResponse from(OrderItem orderItem) {
        return new PendingReviewResponse(
                orderItem.getId(),
                orderItem.getProduct().getProductCode(),
                orderItem.getProductName(),
                orderItem.getOrder().getCreatedAt(),
                orderItem.getProduct().getImageUrl()
        );
    }
}