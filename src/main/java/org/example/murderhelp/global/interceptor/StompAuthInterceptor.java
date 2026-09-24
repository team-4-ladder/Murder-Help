package org.example.murderhelp.global.interceptor;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.global.jwt.JwtProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final StompSubscribeAuthChecker subscribeAuthChecker;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();

        // ── CONNECT: JWT 검증 후 memberId를 세션에 저장 ──────────────────────
        if (StompCommand.CONNECT.equals(command)) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtProvider.validate(token) && !jwtProvider.isRefreshToken(token)) {
                    Long memberId = jwtProvider.getMemberId(token);
                    accessor.getSessionAttributes().put("memberId", memberId);
                    accessor.setUser(() -> String.valueOf(memberId));
                } else {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }
            } else {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }

        // ── SUBSCRIBE: destination별 인가 검증 ───────────────────────────────
        if (StompCommand.SUBSCRIBE.equals(command)) {
            Long memberId = (Long) accessor.getSessionAttributes().get("memberId");
            if (memberId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            String destination = accessor.getDestination();
            if (destination == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }

            subscribeAuthChecker.check(destination, memberId);
        }

        return message;
    }
}

