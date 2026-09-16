package org.example.murderhelp.domain.chat.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.service.ChatCacheRecoveryService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 채팅 캐시 기동 워밍업 리스너
 * - test 프로파일에서는 빈 등록 자체를 제외하여 Redis 접근을 완전히 차단
 * - 실제 복구 로직은 ChatCacheRecoveryService.restoreOnStartup()에 위임
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ChatCacheWarmupListener {

    private final ChatCacheRecoveryService chatCacheRecoveryService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[ChatCacheWarmupListener] 채팅 캐시 기동 복구 시작...");
        chatCacheRecoveryService.restoreOnStartup();
    }
}
