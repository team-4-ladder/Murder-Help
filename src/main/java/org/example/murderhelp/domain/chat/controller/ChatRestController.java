package org.example.murderhelp.domain.chat.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.dto.CursorPageResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.global.response.ApiResponse;
import org.example.murderhelp.domain.chat.facade.ChatFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRestController {

    private final ChatFacade chatFacade;


    /**
     * 신규 상담 채팅방을 생성한다. 이미 진행 중인 방이 있으면 방 생성을 거부하고,
     * 성공 시 챗봇 환영 메시지 발송까지 한 번에 처리된다.
     *
     * @param memberId 인증된 요청자(고객)의 회원 ID
     * @return 생성된 채팅방 정보
     */
    @PostMapping
    public ApiResponse<ChatRoomResponse> createRoom(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.ok(chatFacade.createRoomAndSendGreeting(memberId));
    }

    /**
     * 로그인한 고객 본인의 채팅방 목록을 상태/키워드 조건으로 페이징 조회한다.
     *
     * @param memberId 인증된 요청자(고객)의 회원 ID — 이 값 소유의 방만 조회 대상
     * @param status   방 상태 필터 (미지정 시 전체)
     * @param keyword  방 제목/고객 이름/이메일 검색어 (미지정 시 전체)
     * @param pageable 페이징 정보
     * @return 조건에 맞는 채팅방 목록 페이지
     */
    @GetMapping("/my")
    public ApiResponse<Page<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) ChatRoomStatus status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        
        // 내 방만 조회
        return ApiResponse.ok(chatFacade.getRooms(memberId, status, keyword, pageable));
    }


    /**
     * 관리자(GREEN 등급) 전용 — 전체 채팅방 목록을 고객/상태/키워드 조건으로 검색한다.
     *
     * @param customerId 특정 고객으로 좁혀볼 때 사용 (미지정 시 전체 고객 대상)
     * @param status     방 상태 필터 (미지정 시 전체)
     * @param keyword    방 제목/고객 이름/이메일 검색어 (미지정 시 전체)
     * @param pageable   페이징 정보
     * @return 조건에 맞는 채팅방 목록 페이지
     */
    @PreAuthorize("hasRole('GREEN')")
    @GetMapping
    public ApiResponse<Page<ChatRoomResponse>> getAllRooms(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) ChatRoomStatus status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        return ApiResponse.ok(chatFacade.getRooms(customerId, status, keyword, pageable));
    }

    /**
     * 채팅방 단건 상세를 조회한다. 요청자가 해당 방의 고객이거나 관리자가 아니면 접근이 거부된다.
     *
     * @param memberId 인증된 요청자의 회원 ID
     * @param roomId   조회할 채팅방 ID
     * @return 채팅방 상세 정보
     */
    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomResponse> getRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId) {
        return ApiResponse.ok(chatFacade.getRoom(roomId, memberId));
    }


    /**
     * 특정 채팅방의 과거 메시지 내역을 커서 기반으로 최신순 조회한다.
     *
     * @param memberId      인증된 요청자의 회원 ID (접근 권한 검증에 사용)
     * @param roomId        조회할 채팅방 ID
     * @param lastMessageId 이전 응답의 nextCursorId (최초 조회 시 미지정)
     * @param size          조회할 메시지 개수 (기본 20)
     * @return 커서 기반 메시지 내역 페이지
     */
    @GetMapping("/{roomId}/messages")
    public ApiResponse<CursorPageResponse<ChatMessageResponse>> getMessageHistory(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(chatFacade.getMessageHistory(roomId, memberId, lastMessageId, size));
    }


    /**
     * 상담을 종료 처리한다. 요청자가 해당 방의 고객 또는 관리자여야 하며,
     * 처리 후 방 상태는 COMPLETED로 전이되고 종료 시스템 메시지가 발송된다.
     *
     * @param memberId 인증된 요청자의 회원 ID
     * @param roomId   종료할 채팅방 ID
     */
    @PatchMapping("/{roomId}/close")
    public ApiResponse<Void> closeRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId) {
        chatFacade.closeRoom(roomId, memberId);
        return ApiResponse.ok();
    }


    /**
     * 관리자(GREEN 등급) 전용 — Redis "chat_last_messages" 캐시를 DB 기준으로 수동 재적재한다.
     * 캐시 유실/불일치가 의심될 때 운영자가 직접 트리거하는 복구용 엔드포인트다.
     *
     * @return 복구된 채팅방 수를 포함한 안내 메시지
     */
    @PreAuthorize("hasRole('GREEN')")
    @PostMapping("/cache/recover")
    public ApiResponse<String> recoverCache() {
        int count = chatFacade.recoverCache();
        return ApiResponse.ok(count + "개 채팅방 캐시 복구 완료");
    }
}
