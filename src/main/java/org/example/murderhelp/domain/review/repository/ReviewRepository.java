package org.example.murderhelp.domain.review.repository;

import java.util.Collection;
import java.util.List;
import org.example.murderhelp.domain.review.dto.ReviewStats;
import org.example.murderhelp.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Review> findAllByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByOrderItemId(Long orderItemId);

    List<Review> findAllByOrderItemIdIn(Collection<Long> orderItemIds);

    // 인기 상품 랭킹 후보군의 리뷰 수/평균 평점을 상품별로 집계 (Review.productId는 연관관계가 아닌 단순 컬럼이라 조인 대신 IN + GROUP BY 사용)
    @Query("""
        SELECT new org.example.murderhelp.domain.review.dto.ReviewStats(r.productId, COUNT(r.id), AVG(r.rating))
        FROM Review r
        WHERE r.productId IN :productIds
        GROUP BY r.productId
        """)
    List<ReviewStats> findReviewStatsByProductIds(@Param("productIds") List<Long> productIds);
}
