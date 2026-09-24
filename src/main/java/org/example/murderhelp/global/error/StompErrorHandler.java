package org.example.murderhelp.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

/**
 * STOMP 채널(인터셉터·메시지 처리)에서 발생한 예외를 클라이언트에게
 * 정제된 STOMP ERROR 프레임으로 전달하는 핸들러.
 */
@Slf4j
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    /**
     * 클라이언트 메시지 처리 중 발생한 예외를 STOMP ERROR 프레임으로 변환한다.
     *
     * @param clientMessage 예외가 발생한 원본 클라이언트 메시지 (null일 수 있음)
     * @param ex            발생한 예외
     * @return 클라이언트에게 전송할 STOMP ERROR 메시지
     */
    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        if (cause instanceof BusinessException businessException) {
            log.warn("[STOMP] BusinessException: {}", businessException.getMessage());
            return buildErrorMessage(
                    businessException.getErrorCode().getCode(),
                    businessException.getMessage()
            );
        }

        log.error("[STOMP] 처리되지 않은 예외 발생", cause);
        return buildErrorMessage(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
    }

    /**
     * STOMP ERROR 프레임을 생성한다.
     * message 헤더에 errorCode, body에 상세 메시지를 담아 클라이언트가 파싱할 수 있게 한다.
     */
    private Message<byte[]> buildErrorMessage(String errorCode, String message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorCode);
        accessor.setLeaveMutable(true);

        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
}
