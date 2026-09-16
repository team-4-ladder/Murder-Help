package org.example.murderhelp.domain.chat.event;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.redis.ChatLastMessageCache;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatRedisEventListener {

    private final ChatRedisPublisher chatRedisPublisher;
    private final ChatLastMessageCache chatLastMessageCache;

    /**
     * 메시지 저장 트랜잭션이 커밋된 직후(AFTER_COMMIT)에 실행된다.
     * 저장된 메시지를 Redis "chat-room:{roomId}" 채널로 발행해, 해당 방을 구독 중인
     * 모든 서버 인스턴스의 웹소켓 클라이언트에게 실시간으로 전달되게 한다.
     *
     * @param event 저장된 메시지와 방 ID를 담은 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageCreatedEvent(ChatMessageCreatedEvent event) {
        chatRedisPublisher.publish(event.roomId(), ChatMessageResponse.from(event.message()));
    }

    /**
     * 방 상태/정보 변경 트랜잭션이 커밋된 직후(AFTER_COMMIT)에 실행된다.
     * 캐시된 마지막 메시지를 함께 조회해 붙인 뒤, Redis "chat-room:updates" 채널로 발행해
     * 관리자 대시보드 등 방 목록을 구독 중인 클라이언트에 실시간 반영한다.
     *
     * @param event 변경된 채팅방 엔티티를 담은 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomUpdatedEvent(ChatRoomUpdatedEvent event) {
        String lastMsg = chatLastMessageCache.get(event.room().getId());
        chatRedisPublisher.publishRoomUpdate(ChatRoomResponse.from(event.room(), lastMsg));
    }
}
