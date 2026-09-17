package org.example.murderhelp.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.dto.ProductSalesCount;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemQueryService {

    private final OrderItemRepository orderItemRepository;

    /**
     * 기준 시각(since) 이후 특정 상태(status)로 처리된 주문 중, 지정된 상품 등급(allowedTiers)에 속한
     * 상품들의 판매량 상위 limit개를 판매량 내림차순으로 조회한다.
     */
    public List<ProductSalesCount> findTopSellingProducts(
            LocalDateTime since,
            OrderStatus status,
            List<ProductTier> allowedTiers,
            int limit
    ) {
        return orderItemRepository.findTopSellingProductsByTiersSince(
                        since, status, allowedTiers, PageRequest.of(0, limit))
                .stream()
                .map(row -> new ProductSalesCount((Long) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
