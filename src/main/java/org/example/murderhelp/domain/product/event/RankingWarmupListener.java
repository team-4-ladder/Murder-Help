package org.example.murderhelp.domain.product.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.product.service.ProductRankingService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 인기 상품 랭킹 기동 워밍업 리스너
 * - test 프로파일에서는 빈 등록 자체를 제외하여 Redis 접근을 완전히 차단
 * - 실제 워밍업 로직은 ProductRankingService.warmUpOnStartup()에 위임
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RankingWarmupListener {

    private final ProductRankingService productRankingService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[RankingWarmupListener] 인기 상품 랭킹 캐시 기동 워밍업 시작...");
        productRankingService.warmUpOnStartup();
    }
}
