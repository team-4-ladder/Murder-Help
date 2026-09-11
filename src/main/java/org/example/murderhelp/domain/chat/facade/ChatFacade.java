package org.example.murderhelp.domain.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.dto.ChatRoomCreateRequest;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.chat.service.ChatRoomService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /**
     * 방 생성 + 챗봇 첫 인사 발송 오케스트레이션
     */
    public ChatRoomResponse createRoomAndSendGreeting(ChatRoomCreateRequest request) {
        log.info("========== [채팅방 생성 파사드 진입] ==========");

        ChatRoomResponse roomResponse = chatRoomService.createRoom(request);

        chatMessageService.sendBotWelcomeMessage(roomResponse.roomId());

        log.info("채팅방 생성 및 챗봇 환영 메시지 발송 완료. Room ID: {}", roomResponse.roomId());
        
        return roomResponse;
    }
}
