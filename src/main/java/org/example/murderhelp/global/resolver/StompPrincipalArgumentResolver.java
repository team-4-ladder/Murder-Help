package org.example.murderhelp.global.resolver;

import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @StompMemberId 파라미터를, StompAuthInterceptor가 CONNECT 시점에 세션에 저장해둔
 * memberId 값으로 채워준다. 클라이언트가 페이로드에 실어 보낸 memberId는 신뢰하지 않고
 * 오직 인증된 세션 값만 사용한다.
 */
@Component
public class StompPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(StompMemberId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, Message<?> message) {
        Map<String, Object> sessionAttributes = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
        Object memberId = sessionAttributes != null ? sessionAttributes.get("memberId") : null;

        if (!(memberId instanceof Long)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return memberId;
    }
}
