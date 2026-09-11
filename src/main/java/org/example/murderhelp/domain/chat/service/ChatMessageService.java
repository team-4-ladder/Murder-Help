package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.constant.BotScenario;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final ObjectMapper objectMapper;


    @Transactional
    public void sendBotWelcomeMessage(Long roomId) {
        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        
        String botMessageJson;
        try {
            botMessageJson = objectMapper.writeValueAsString(BotScenario.WELCOME.getMessageDto());
        } catch (Exception e) {
            throw new RuntimeException("봇 메시지 생성 실패", e);
        }
        
        ChatMessage botMessage = ChatMessage.builder()
                .chatRoom(room)
                .memberId(0L)
                .content(botMessageJson)
                .messageType(ChatMessageType.BUTTON)
                .build();
        
        chatMessageRepository.save(botMessage);
    }

    @Transactional
    public void sendMessage(ChatMessageSendRequest request) {

        ChatRoom room = chatRoomService.getRoomEntity(request.roomId());

        boolean isCustomer = room.isCustomer(request.memberId());
        
        if (!room.getStatus().canSendMessage(isCustomer)) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }
        if (!isCustomer && room.getStatus() == ChatRoomStatus.WAITING && room.getAdminId() == null) {
            room.assignAdmin(request.memberId());
            // Redis Pub/Sub을 통해 관리자 대시보드로 상태 변경 브로드캐스트
            chatRedisPublisher.publishRoomUpdate(ChatRoomResponse.from(room));
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .memberId(request.memberId())
                .content(request.content())
                .messageType(request.messageType() != null ? request.messageType() : ChatMessageType.TEXT)
                .build();
        
        chatMessageRepository.save(message);

        ChatMessageResponse response = ChatMessageResponse.from(message);
        chatRedisPublisher.publish(room.getId(), response);

        // 챗봇 모드(BOT_MODE) 처리 로직
        if (room.getStatus().isBotActive() && isCustomer) {
            if ("상담사 연결".equals(request.content())) {
                room.changeToWaiting();
                chatRedisPublisher.publishRoomUpdate(ChatRoomResponse.from(room));
                
                ChatMessage botMsg = ChatMessage.builder()
                    .chatRoom(room)
                    .memberId(0L)
                    .content("상담사 연결을 대기 중입니다. 잠시만 기다려주세요.")
                    .messageType(ChatMessageType.SYSTEM)
                    .build();
                chatMessageRepository.save(botMsg);
                chatRedisPublisher.publish(room.getId(), ChatMessageResponse.from(botMsg));
            } else if ("무기 추천".equals(request.content())) {
                String recJson;
                try {
                    recJson = objectMapper.writeValueAsString(BotScenario.RECOMMEND_WEAPON.getMessageDto());
                } catch (Exception e) {
                    throw new RuntimeException("봇 메시지 생성 실패", e);
                }
                ChatMessage botMsg = ChatMessage.builder()
                    .chatRoom(room)
                    .memberId(0L)
                    .content(recJson)
                    .messageType(ChatMessageType.BUTTON)
                    .build();
                chatMessageRepository.save(botMsg);
                chatRedisPublisher.publish(room.getId(), ChatMessageResponse.from(botMsg));
            } else {
                String fallbackJson;
                try {
                    fallbackJson = objectMapper.writeValueAsString(BotScenario.FALLBACK.getMessageDto());
                } catch (Exception e) {
                    throw new RuntimeException("봇 메시지 생성 실패", e);
                }
                ChatMessage botMsg = ChatMessage.builder()
                    .chatRoom(room)
                    .memberId(0L)
                    .content(fallbackJson)
                    .messageType(ChatMessageType.BUTTON)
                    .build();
                chatMessageRepository.save(botMsg);
                chatRedisPublisher.publish(room.getId(), ChatMessageResponse.from(botMsg));
            }
        }

        chatRoomService.updateLastMessageTime(room.getId());
    }

    public Page<ChatMessageResponse> getMessageHistory(Long roomId, Pageable pageable) {
        return chatMessageRepository.findMessagesByRoomId(roomId, pageable)
                .map(ChatMessageResponse::from);
    }
}
