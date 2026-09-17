package org.example.murderhelp.domain.review.dto;

public record ReviewStats(
    Long productId,
    Long reviewCount,
    Double avgRating
) {}
