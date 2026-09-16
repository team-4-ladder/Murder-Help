package org.example.murderhelp.global.resolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * STOMP CONNECT 시점에 StompAuthInterceptor가 세션에 저장해둔 memberId를
 * @MessageMapping 핸들러 파라미터로 바로 주입받기 위한 어노테이션.
 * REST의 @AuthenticationPrincipal 과 대응된다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface StompMemberId {
}
