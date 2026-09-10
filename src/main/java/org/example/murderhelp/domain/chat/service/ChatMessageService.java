package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRedisPublisher chatRedisPublisher;

    @Transactional
    public void sendMessage(ChatMessageSendRequest request) {

        ChatRoom room = chatRoomService.getRoomEntity(request.roomId());

        room.getStatus().validateMessageSendable();

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .memberId(request.memberId())
                .content(request.content())
                .build();
        
        chatMessageRepository.save(message);
        chatRoomService.updateLastMessageTime(room.getId());

        // Redis Pub/Sub을 통해 메시지 발행 (다중 서버 브로드캐스트)
        ChatMessageResponse response = ChatMessageResponse.from(message);
        chatRedisPublisher.publish(room.getId(), response);
    }

    public Page<ChatMessageResponse> getMessageHistory(Long roomId, Pageable pageable) {
        return chatMessageRepository.findMessagesByRoomId(roomId, pageable)
                .map(ChatMessageResponse::from);
    }
}
