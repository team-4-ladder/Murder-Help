package org.example.murderhelp.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.global.resolver.StompMemberId;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;

    // 실시간 메시지 브로드캐스트 (WebSocket / STOMP)
    /**
     * STOMP로 수신한 채팅 메시지를 처리한다. 클라이언트가 페이로드에 실어 보낸 memberId는
     * 신뢰하지 않고, {@code @StompMemberId}로 주입된 인증 세션의 memberId로 강제 치환한 뒤 저장을 위임한다.
     *
     * @param request  클라이언트가 보낸 원본 메시지 페이로드 (roomId, content, messageType)
     * @param memberId STOMP 세션에 저장된 인증된 발신자 ID
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid ChatMessageSendRequest request,
                            @StompMemberId Long memberId) {

        // 프론트엔드에서 보낸 memberId를 무시하고, 검증된 세션의 memberId를 강제 삽입
        ChatMessageSendRequest secureRequest = new ChatMessageSendRequest(
                request.roomId(),
                memberId,
                request.content(),
                request.messageType()
        );

        chatMessageService.sendMessage(secureRequest);
    }
}
