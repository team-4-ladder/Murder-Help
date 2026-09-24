package org.example.murderhelp.global.interceptor;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.service.ChatRoomService;
import org.example.murderhelp.domain.member.entity.Grade;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * STOMP SUBSCRIBE 커맨드에 대한 인가(Authorization) 검증 전용 컴포넌트.
 *
 */
@Component
@RequiredArgsConstructor
public class StompSubscribeAuthChecker {

    private final ChatRoomService chatRoomService;
    private final MemberService memberService;

    private static final String ROOM_SUB_PREFIX = "/sub/chat/room/";
    private static final String ROOM_UPDATES_DEST = "/sub/chat/rooms/updates";
    private static final String ROOM_STATUS_SUFFIX = "/status";
    private static final String USER_DEST_PREFIX = "/user/";

    /**
     * destination에 따라 적절한 인가 검증을 수행한다.
     *
     * @param destination STOMP SUBSCRIBE 목적지
     * @param memberId    세션에서 꺼낸 인증된 회원 ID
     * @throws BusinessException destination이 허용되지 않거나 접근 권한이 없는 경우
     */
    public void check(String destination, Long memberId) {
        if (destination.startsWith(ROOM_SUB_PREFIX) && destination.endsWith(ROOM_STATUS_SUFFIX)) {
            // /sub/chat/room/{roomId}/status — 고객(방 주인)만 허용
            validateCustomerOnly(destination, memberId);
        } else if (destination.startsWith(ROOM_SUB_PREFIX)) {
            // /sub/chat/room/{roomId} — 고객(방 주인) or 관리자(GREEN) 허용
            validateRoomAccess(destination, memberId);
        } else if (ROOM_UPDATES_DEST.equals(destination)) {
            // /sub/chat/rooms/updates — 관리자(GREEN)만 허용
            validateAdminGrade(memberId);
        } else if (destination.startsWith(USER_DEST_PREFIX)) {
            // /user/queue/errors 등 — Spring UserDestinationMessageHandler가 세션 Principal
            // 기준으로 개인화해서 라우팅하므로 추가 검증 없이 허용(타인 큐 구독 자체가 불가능)
            return;
        } else {
            // 알려진 채팅 destination이 아니면 기본 차단
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * destination에서 roomId를 파싱해 해당 방의 고객 본인 또는 관리자(GREEN)인지 검증한다.
     * 판정 규칙은 ChatRoomService.validateRoomAccess()를 그대로 재사용한다(REST와 동일 규칙 보장).
     */
    private void validateRoomAccess(String destination, Long memberId) {
        long roomId = parseRoomId(destination.substring(ROOM_SUB_PREFIX.length()));

        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        chatRoomService.validateRoomAccess(room, memberId);
    }

    /**
     * /sub/chat/room/{roomId}/status 전용 — 해당 방의 고객(방 주인)만 허용한다.
     * 관리자(GREEN)는 /sub/chat/rooms/updates로 방 상태를 수신하므로 이 채널에선 제외한다.
     */
    private void validateCustomerOnly(String destination, Long memberId) {
        String segment = destination.substring(ROOM_SUB_PREFIX.length()); // "{roomId}/status"
        String roomIdStr = segment.substring(0, segment.length() - ROOM_STATUS_SUFFIX.length());
        long roomId = parseRoomId(roomIdStr);

        ChatRoom room = chatRoomService.getRoomEntity(roomId);

        if (!room.isCustomer(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private long parseRoomId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 요청자가 GREEN 등급(관리자)인지 검증한다.
     */
    private void validateAdminGrade(Long memberId) {
        Member requester = memberService.getMemberById(memberId);

        if (requester.getGrade() != Grade.GREEN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
