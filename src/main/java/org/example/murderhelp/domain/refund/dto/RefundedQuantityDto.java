package org.example.murderhelp.domain.refund.dto;

public record RefundedQuantityDto(
        Long orderItemId,
        int refundedQuantity
) {}