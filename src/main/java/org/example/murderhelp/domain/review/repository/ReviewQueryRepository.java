package org.example.murderhelp.domain.review.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepository {

    private final EntityManager entityManager;

    public List<OrderItem> findPendingReviewItems(Long memberId) {
        return entityManager.createQuery("""
                        SELECT oi
                        FROM OrderItem oi
                        JOIN FETCH oi.order o
                        JOIN FETCH oi.product p
                        WHERE o.member.id = :memberId
                          AND o.status = :status
                          AND NOT EXISTS (
                              SELECT r.id
                              FROM Review r
                              WHERE r.orderItemId = oi.id
                          )
                        ORDER BY o.createdAt DESC, oi.id DESC
                        """, OrderItem.class)
                .setParameter("memberId", memberId)
                .setParameter("status", OrderStatus.DELIVERED)
                .getResultList();
    }
}