package org.example.murderhelp.domain.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.review.dto.ReviewStats;
import org.example.murderhelp.domain.review.repository.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final StringRedisTemplate redisTemplate;

    public static final String RANKING_TARGET_KEY_PREFIX = "ranking:weekly:best:";
    private static final String RANKING_TEMP_KEY_PREFIX = "ranking:weekly:best:temp:";

    // 판매량만으로는 상위권에 못 들었지만 리뷰가 좋은 상품도 재정렬 대상에 들도록 넉넉히 뽑아두는 후보군 크기
    private static final int CANDIDATE_POOL_SIZE = 20;
    private static final int TOP_N = 5;
    // 최종 점수 = 판매 점수(정규화) * SALES_WEIGHT + 리뷰 점수(정규화) * REVIEW_WEIGHT
    private static final double SALES_WEIGHT = 0.7;
    private static final double REVIEW_WEIGHT = 0.3;
    // 리뷰 수가 이 값 이상이어야 평점을 100% 신뢰도로 반영 (그 미만이면 비례 감쇠)
    private static final int REVIEW_CONFIDENCE_THRESHOLD = 5;

    /**
     * 기동 시 조건부 워밍업 — RankingWarmupListener에 의해 호출됨 (test 프로파일 제외)
     * 등급별 Redis 키가 하나라도 없을 때만 updateWeeklyBestProducts() 실행
     */
    public void warmUpOnStartup() {
        log.info("[랭킹 워밍업] 서버 기동 — Redis 랭킹 캐시 상태 확인 중...");

        boolean anyMissing = Arrays.stream(ProductTier.values())
                .filter(tier -> tier != ProductTier.GREEN)
                .anyMatch(tier -> {
                    String key = RANKING_TARGET_KEY_PREFIX + tier.name().toLowerCase();
                    return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
                });

        if (!anyMissing) {
            log.info("[랭킹 워밍업] 모든 등급 랭킹 캐시가 이미 존재합니다. 워밍업을 건너뜁니다.");
            return;
        }

        log.info("[랭킹 워밍업] 랭킹 캐시 미존재 감지 — updateWeeklyBestProducts() 실행합니다.");
        updateWeeklyBestProducts();
        log.info("[랭킹 워밍업] 인기 상품 랭킹 캐시 워밍업 완료.");
    }

    @Transactional(readOnly = true)
    public void updateWeeklyBestProducts() {
        log.info("[랭킹 스케줄러] 주간 베스트 무기 랭킹(등급별) 집계를 시작합니다.");
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        for (ProductTier userTier : ProductTier.values()) {
            if (userTier == ProductTier.GREEN) {
                continue; // GREEN 등급은 관리자 전용이므로 인기 상품 집계에서 제외합니다.
            }

            // 해당 등급의 유저가 볼 수 있는 무기 등급 리스트
            List<ProductTier> allowedTiers = Arrays.stream(ProductTier.values())
                    .filter(userTier::canAccess)
                    .collect(Collectors.toList());

            List<Object[]> topSelling = orderItemRepository.findTopSellingProductsByTiersSince(
                    startDate,
                    OrderStatus.DELIVERED,
                    allowedTiers,
                    PageRequest.of(0, CANDIDATE_POOL_SIZE)
            );

            String targetKey = RANKING_TARGET_KEY_PREFIX + userTier.name().toLowerCase();
            String tempKey = RANKING_TEMP_KEY_PREFIX + userTier.name().toLowerCase();

            if (topSelling.isEmpty()) {
                log.info("[랭킹 스케줄러] 최근 7일간 판매된 데이터가 없어 {} 등급 기존 랭킹 캐시를 초기화합니다.", userTier.name());
                redisTemplate.delete(targetKey);
                continue;
            }

            List<String> productIds = rankByScore(topSelling);

            redisTemplate.delete(tempKey);
            redisTemplate.opsForList().rightPushAll(tempKey, productIds);
            redisTemplate.rename(tempKey, targetKey);
            
            log.info("[랭킹 스케줄러] {} 등급 주간 베스트 무기 랭킹 갱신 완료! (상품 ID: {})", userTier.name(), productIds);
        }
    }

    /**
     * 판매량 후보군(candidatePool)을 판매 점수 + 리뷰 점수 가중합으로 재정렬해 상위 TOP_N개의 상품 ID를 반환한다.
     * 판매 점수는 후보군 내 min-max 정규화, 리뷰 점수는 평균 평점을 리뷰 수 기반 신뢰도로 감쇠해 산출한다.
     */
    private List<String> rankByScore(List<Object[]> candidatePool) {
        record Candidate(Long productId, long salesQty) {}

        List<Candidate> candidates = candidatePool.stream()
                .map(row -> new Candidate((Long) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<Long> candidateIds = candidates.stream().map(Candidate::productId).toList();
        Map<Long, ReviewStats> reviewStatsByProductId = reviewRepository.findReviewStatsByProductIds(candidateIds)
                .stream()
                .collect(Collectors.toMap(ReviewStats::productId, stats -> stats));

        long maxSales = candidates.stream().mapToLong(Candidate::salesQty).max().orElse(1);
        long minSales = candidates.stream().mapToLong(Candidate::salesQty).min().orElse(0);
        long salesRange = Math.max(1, maxSales - minSales);

        return candidates.stream()
                .sorted(Comparator.comparingDouble((Candidate c) -> {
                    double salesScore = (double) (c.salesQty() - minSales) / salesRange;

                    ReviewStats stats = reviewStatsByProductId.get(c.productId());
                    double avgRating = stats != null && stats.avgRating() != null ? stats.avgRating() : 0.0;
                    long reviewCount = stats != null && stats.reviewCount() != null ? stats.reviewCount() : 0L;
                    double confidence = Math.min(1.0, (double) reviewCount / REVIEW_CONFIDENCE_THRESHOLD);
                    double reviewScore = (avgRating / 5.0) * confidence;

                    return salesScore * SALES_WEIGHT + reviewScore * REVIEW_WEIGHT;
                }).reversed())
                .limit(TOP_N)
                .map(c -> String.valueOf(c.productId()))
                .toList();
    }
}
