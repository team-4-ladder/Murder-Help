package org.example.murderhelp.domain.refund.dto;

import org.example.murderhelp.domain.refund.entity.Refund;

import java.time.LocalDateTime;
import java.util.List;

public record RefundHistoryResponse(
        Long refundId,
        LocalDateTime refundDate,
        String status,
        Long totalRefundAmount,
        Long pgRefundAmount,
        List<RefundHistoryItemResponse> items
) {
    public static RefundHistoryResponse from(Refund refund) {
        return new RefundHistoryResponse(
                refund.getId(),
                refund.getCreatedAt(),
                refund.getStatus().name(),
                refund.getPgRefundAmount(),
                refund.getPgRefundAmount(),
                refund.getRefundItems().stream()
                        .map(RefundHistoryItemResponse::from)
                        .toList()
        );
    }
}