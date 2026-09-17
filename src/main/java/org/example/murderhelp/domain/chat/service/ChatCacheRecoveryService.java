package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.redis.ChatLastMessageCache;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import org.example.murderhelp.domain.chat.repository.ChatRoomRepository;
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
    private final ChatLastMessageCache chatLastMessageCache;

    /**
     * 애플리케이션 기동 시 ChatCacheWarmupListener가 호출하는 조건부 캐시 복구 진입점이다.
     * Redis에 캐시된 항목 수가 활성(미종료) 채팅방 수 이상이면 이미 정상 상태로 보고 건너뛰고,
     * 그렇지 않으면 {@link #restore()}로 전체 복구를 수행한다.
     */
    public void restoreOnStartup() {
        log.info("[채팅 캐시 복구] 시작 — Redis chat_last_messages 상태 확인 중...");

        long activeRoomCount = chatRoomRepository.countByStatusNot(ChatRoomStatus.COMPLETED);
        long cachedCount = chatLastMessageCache.size();

        if (cachedCount >= activeRoomCount && activeRoomCount > 0) {
            log.info("[채팅 캐시 복구] Redis 캐시 정상 (활성 방: {}개 / 캐시: {}개). 복구를 건너뜁니다.",
                    activeRoomCount, cachedCount);
            return;
        }

        int count = restore();
        log.info("[채팅 캐시 복구] 완료 — {}개 채팅방 복구됨", count);
    }

    /**
     * COMPLETED가 아닌 모든 활성 채팅방의 마지막 메시지를 DB에서 다시 조회해
     * Redis "chat_last_messages" 캐시에 일괄 재적재한다. 기동 시 자동 호출 외에
     * 관리자가 API로 수동 트리거할 수도 있다.
     *
     * @return 복구(재적재)된 채팅방 수
     */
    public int restore() {
        List<ChatRoom> activeRooms = chatRoomRepository.findAllByStatusNot(ChatRoomStatus.COMPLETED);

        if (activeRooms.isEmpty()) {
            log.info("[채팅 캐시 복구] 활성 채팅방 없음");
            return 0;
        }

        List<Long> roomIds = activeRooms.stream()
                .map(ChatRoom::getId)
                .toList();

        List<ChatMessage> lastMessages = chatMessageRepository.findLastMessagesByRoomIds(roomIds);

        lastMessages.forEach(msg -> {
            String preview = msg.getMessageType() == ChatMessageType.BUTTON
                    ? "[챗봇 메시지]"
                    : msg.getContent();
            chatLastMessageCache.put(msg.getChatRoom().getId(), preview);
        });

        return lastMessages.size();
    }
}
