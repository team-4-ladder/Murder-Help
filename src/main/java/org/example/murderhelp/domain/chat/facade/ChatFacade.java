package org.example.murderhelp.domain.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.dto.CursorPageResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.service.ChatCacheRecoveryService;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.chat.service.ChatRoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatCacheRecoveryService chatCacheRecoveryService;

    /**
     * [방 생성 오케스트레이션] 채팅방 생성 → 챗봇 환영 메시지 발송을 하나의 트랜잭션으로 묶어 처리한다.
     * 컨트롤러는 이 메서드 하나만 호출하면 되고, 두 단계 사이의 순서/실패 시 롤백은 이 메서드가 책임진다.
     *
     * @param memberId 채팅방을 개설하는 고객의 회원 ID
     * @return 생성된 채팅방 정보
     */
    public ChatRoomResponse createRoomAndSendGreeting(Long memberId) {
        log.info("========== [채팅방 생성 파사드 진입] ==========");

        ChatRoomResponse roomResponse = chatRoomService.createRoom(memberId);

        chatMessageService.sendBotWelcomeMessage(roomResponse.roomId());

        log.info("채팅방 생성 및 챗봇 환영 메시지 발송 완료. Room ID: {}", roomResponse.roomId());
        
        return roomResponse;
    }

    /**
     * [방 종료 오케스트레이션] 접근 권한 검증 → 방 상태를 COMPLETED로 전이 → 종료 시스템 메시지 발송까지
     * 한 트랜잭션으로 조율한다. 조회한 방 엔티티를 그대로 재사용해 중복 조회 없이 각 단계에 전달한다.
     *
     * @param roomId   종료할 채팅방 ID
     * @param memberId 요청자 회원 ID (고객 본인 또는 관리자만 허용)
     */
    public void closeRoom(Long roomId, Long memberId) {
        log.info("========== [채팅방 종료 파사드 진입] ==========");

        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        chatRoomService.validateRoomAccess(room, memberId);

        chatRoomService.closeRoom(room);
        chatMessageService.sendCloseSystemMessage(room);

        log.info("채팅방 종료 및 시스템 메시지 발송 완료. Room ID: {}", roomId);
    }

    /**
     * 채팅방 목록 조회를 위임한다 (읽기 전용 트랜잭션). 고객용/관리자용 목록 조회 엔드포인트가
     * 공통으로 사용하는 단일 진입점이다.
     *
     * @param customerId 고객 필터 (null이면 전체 대상 — 관리자 전체 조회용)
     * @param status     방 상태 필터
     * @param keyword    검색 키워드
     * @param pageable   페이징 정보
     * @return 조건에 맞는 채팅방 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<ChatRoomResponse> getRooms(Long customerId, ChatRoomStatus status, String keyword, Pageable pageable) {
        return chatRoomService.getRooms(customerId, status, keyword, pageable);
    }

    /**
     * 채팅방 단건 조회 + 접근 권한 검증을 위임한다 (읽기 전용 트랜잭션).
     *
     * @param roomId   조회할 채팅방 ID
     * @param memberId 요청자 회원 ID
     * @return 채팅방 상세 정보
     */
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long roomId, Long memberId) {
        return chatRoomService.getRoom(roomId, memberId);
    }

    /**
     * 채팅방 메시지 내역의 커서 기반 조회를 위임한다 (읽기 전용 트랜잭션).
     *
     * @param roomId        조회할 채팅방 ID
     * @param memberId      요청자 회원 ID (접근 권한 검증에 사용)
     * @param lastMessageId 이전 응답의 nextCursorId (최초 조회 시 null)
     * @param size          조회할 메시지 개수
     * @return 커서 기반 메시지 내역 페이지
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ChatMessageResponse> getMessageHistory(Long roomId, Long memberId, Long lastMessageId, int size) {
        return chatMessageService.getMessageHistory(roomId, memberId, lastMessageId, size);
    }

    /**
     * Redis 마지막 메시지 캐시의 수동 복구를 위임한다 (읽기 전용 트랜잭션 — Redis 쓰기만 발생, DB 변경 없음).
     *
     * @return 복구된 채팅방 수
     */
    @Transactional(readOnly = true)
    public int recoverCache() {
        return chatCacheRecoveryService.restore();
    }
}
