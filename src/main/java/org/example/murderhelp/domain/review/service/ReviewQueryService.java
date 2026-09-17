package org.example.murderhelp.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.review.dto.ReviewStats;
import org.example.murderhelp.domain.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;

    /**
     * 상품 ID 목록에 대한 리뷰 수/평균 평점을 상품별로 집계해 조회한다.
     */
    public Map<Long, ReviewStats> getReviewStats(List<Long> productIds) {
        return reviewRepository.findReviewStatsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ReviewStats::productId, stats -> stats));
    }
}
