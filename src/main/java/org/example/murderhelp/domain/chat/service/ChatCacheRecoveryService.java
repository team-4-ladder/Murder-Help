package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import org.example.murderhelp.domain.chat.repository.ChatRoomRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatCacheRecoveryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String LAST_MESSAGES_KEY = "chat_last_messages";

    /**
     * 앱 시작 시 자동 실행 — Redis가 비어있거나 부분 캐시 상태일 경우 DB에서 복구
     * (Redis 항목 수 ≥ 활성 채팅방 수인 경우에만 skip — 부분 캐시 시 복구 실행)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void restoreOnStartup() {
        log.info("[채팅 캐시 복구] 시작 — Redis chat_last_messages 상태 확인 중...");

        long activeRoomCount = chatRoomRepository.countByStatusNot(ChatRoomStatus.COMPLETED);
        Long cachedCount = redisTemplate.opsForHash().size(LAST_MESSAGES_KEY);

        if (cachedCount != null && cachedCount >= activeRoomCount && activeRoomCount > 0) {
            log.info("[채팅 캐시 복구] Redis 캐시 정상 (활성 방: {}개 / 캐시: {}개). 복구를 건너뜁니다.",
                    activeRoomCount, cachedCount);
            return;
        }

        int count = restore();
        log.info("[채팅 캐시 복구] 완료 — {}개 채팅방 복구됨", count);
    }

    /**
     * 관리자 수동 트리거용 (API에서 호출)
     *
     * @return 복구된 채팅방 수
     */
    public int restore() {
        // 1. COMPLETED가 아닌 활성 채팅방 전체 조회
        List<ChatRoom> activeRooms = chatRoomRepository.findAllByStatusNot(ChatRoomStatus.COMPLETED);

        if (activeRooms.isEmpty()) {
            log.info("[채팅 캐시 복구] 활성 채팅방 없음");
            return 0;
        }

        List<Long> roomIds = activeRooms.stream()
                .map(ChatRoom::getId)
                .toList();

        // 2. 각 방의 마지막 메시지를 단일 쿼리로 조회
        List<ChatMessage> lastMessages = chatMessageRepository.findLastMessagesByRoomIds(roomIds);

        // 3. Redis에 일괄 저장
        //    BUTTON 타입(챗봇 메시지)은 JSON 원문 대신 "[챗봇 메시지]" 표기 — sendBotMessage 동일 처리
        lastMessages.forEach(msg -> {
            String preview = msg.getMessageType() == ChatMessageType.BUTTON
                    ? "[챗봇 메시지]"
                    : msg.getContent();
            redisTemplate.opsForHash().put(LAST_MESSAGES_KEY, msg.getChatRoom().getId().toString(), preview);
        });

        return lastMessages.size();
    }
}
