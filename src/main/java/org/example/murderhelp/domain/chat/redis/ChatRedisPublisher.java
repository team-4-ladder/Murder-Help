package org.example.murderhelp.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;

@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 채팅방(topic: "chat-room:{roomId}")으로 메시지를 발행한다.
     * 이 방을 구독 중인 서버 인스턴스의 ChatRedisSubscriber가 수신해
     * 웹소켓 클라이언트로 다시 브로드캐스트한다.
     *
     * @param roomId  발행 대상 채팅방 ID
     * @param message 발행할 메시지 내용
     */
    public void publish(Long roomId, ChatMessageResponse message) {
        String topic = "chat-room:" + roomId;
        redisTemplate.convertAndSend(topic, message);
    }

    /**
     * 채팅방 상태/목록 변경 사항을 공용 토픽("chat-room:updates")으로 발행한다.
     * 관리자 대시보드 등 방 목록을 구독 중인 모든 인스턴스에 전달하기 위함이다.
     *
     * @param response 변경된 채팅방 정보
     */
    public void publishRoomUpdate(ChatRoomResponse response) {
        String topic = "chat-room:updates";
        redisTemplate.convertAndSend(topic, response);
    }
}
