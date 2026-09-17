package org.example.murderhelp.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 챗봇 무기 추천용 등급별 주간 베스트 랭킹("ranking:weekly:best:{tier}") Redis 접근을 이 클래스 하나로 캡슐화한다.
 */
@Component
@RequiredArgsConstructor
public class ChatbotRankingCache {

    private static final String TARGET_KEY_PREFIX = "ranking:weekly:best:";
    private static final String TEMP_KEY_PREFIX = "ranking:weekly:best:temp:";

    private final StringRedisTemplate redisTemplate;

    public boolean exists(ProductTier tier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(targetKey(tier)));
    }

    public List<String> getRanking(ProductTier tier) {
        return redisTemplate.opsForList().range(targetKey(tier), 0, -1);
    }

    /**
     * 임시 키에 랭킹을 적재한 뒤 RENAME으로 원자적으로 교체해, 갱신 도중 빈 값이 노출되는 것을 방지한다.
     */
    public void replaceAtomically(ProductTier tier, List<String> productIds) {
        String tempKey = tempKey(tier);
        redisTemplate.delete(tempKey);
        redisTemplate.opsForList().rightPushAll(tempKey, productIds);
        redisTemplate.rename(tempKey, targetKey(tier));
    }

    public void evict(ProductTier tier) {
        redisTemplate.delete(targetKey(tier));
    }

    private String targetKey(ProductTier tier) {
        return TARGET_KEY_PREFIX + tier.getValue();
    }

    private String tempKey(ProductTier tier) {
        return TEMP_KEY_PREFIX + tier.getValue();
    }
}
