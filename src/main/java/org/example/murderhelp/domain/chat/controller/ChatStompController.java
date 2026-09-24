package org.example.murderhelp.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.resolver.StompMemberId;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
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

    /**
     * @MessageMapping 처리 중 발생한 BusinessException을 잡아
     * 요청을 보낸 클라이언트의 /user/queue/errors 채널로 에러 응답을 전달한다.
     * GlobalExceptionHandler는 HTTP 파이프라인 전용이므로 STOMP 예외는 여기서 별도 처리한다.
     *
     * @param e 발생한 비즈니스 예외
     * @return 에러 코드와 메시지를 담은 ApiResponse
     */
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }
}
