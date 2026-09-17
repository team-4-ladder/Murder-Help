package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.event.ChatRoomUpdatedEvent;
import org.example.murderhelp.domain.chat.redis.ChatLastMessageCache;
import org.example.murderhelp.domain.chat.repository.ChatRoomRepository;
import org.example.murderhelp.domain.member.entity.Grade;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final MemberService memberService;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatLastMessageCache chatLastMessageCache;

    /**
     * 신규 채팅방을 생성한다. 동일 고객에게 WAITING/IN_PROGRESS 상태의 활성 방이 이미 있으면
     * 생성을 거부한다(1인 1활성방 정책). 생성 직후 방 상태는 BOT_MODE로 시작한다.
     *
     * @param memberId 채팅방을 개설하는 고객의 회원 ID
     * @return 생성된 채팅방 정보
     * @throws BusinessException 이미 활성 상태의 방이 존재하는 경우
     */
    @Transactional
    public ChatRoomResponse createRoom(Long memberId) {
        boolean hasActiveRoom = chatRoomRepository.existsByCustomerIdAndStatusIn(
                memberId,
                List.of(ChatRoomStatus.WAITING, ChatRoomStatus.IN_PROGRESS)
        );

        if (hasActiveRoom) {
            throw new BusinessException(ErrorCode.ALREADY_ACTIVE_ROOM_EXISTS);
        }

        Member customer = memberService.getMemberById(memberId);

        String randomHash = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String generatedTitle = String.format("REQ-CUST%03d-%s", memberId, randomHash);

        ChatRoom room = ChatRoom.builder()
                .title(generatedTitle)
                .customer(customer)
                .build();
        
        chatRoomRepository.save(room);
        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));

        return ChatRoomResponse.from(room, null);
    }

    /**
     * 채팅방을 COMPLETED 상태로 전이시킨다. 이미 종료된 방은 엔티티 내부 상태 검증에서 거부된다.
     * 호출자가 이미 조회해둔 엔티티를 그대로 받아 중복 조회를 피한다.
     *
     * @param room 종료 처리할 채팅방 엔티티
     */
    @Transactional
    public void closeRoom(ChatRoom room) {
        room.closeRoom();

        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));
    }


    /**
     * 조건에 맞는 채팅방 목록을 조회하고, 각 방의 마지막 메시지 미리보기를
     * Redis 캐시에서 일괄 조회해 채워 넣는다.
     *
     * @param customerId 고객 필터 (null이면 전체 대상)
     * @param status     방 상태 필터
     * @param keyword    방 제목/고객 이름/이메일 검색어
     * @param pageable   페이징 정보
     * @return 마지막 메시지가 채워진 채팅방 목록 페이지
     */
    public Page<ChatRoomResponse> getRooms(Long customerId, ChatRoomStatus status, String keyword, Pageable pageable) {
        Page<ChatRoom> roomPage = chatRoomRepository.findRoomsByCondition(customerId, status, keyword, pageable);
        List<String> roomIds = roomPage.stream()
                                       .map(room -> room.getId().toString())
                                       .collect(Collectors.toList());

        Map<String, String> lastMsgMap = chatLastMessageCache.multiGet(roomIds);

        return roomPage.map(room ->
                ChatRoomResponse.from(room, lastMsgMap.get(room.getId().toString())));
    }


    /**
     * 채팅방 단건을 조회하고 요청자의 접근 권한을 검증한다.
     *
     * @param roomId   조회할 채팅방 ID
     * @param memberId 요청자 회원 ID
     * @return 채팅방 상세 정보
     */
    public ChatRoomResponse getRoom(Long roomId, Long memberId) {
        ChatRoom room = getRoomEntity(roomId);
        validateRoomAccess(room, memberId);
        return ChatRoomResponse.from(room);
    }

    /**
     * 채팅방 엔티티를 고객 정보(customer)까지 즉시 로딩(fetch join)한 상태로 조회한다.
     * 채팅 도메인의 단건 조회 진입점으로, 트랜잭션 종료 후에도(AFTER_COMMIT 이벤트 등)
     * customer 지연 로딩 접근이 안전하도록 보장한다.
     *
     * @param roomId 조회할 채팅방 ID
     * @return 채팅방 엔티티
     * @throws BusinessException 해당 ID의 방이 없는 경우
     */
    public ChatRoom getRoomEntity(Long roomId) {
        return chatRoomRepository.findByIdWithCustomer(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * 채팅방 접근 권한 검증
     * - GREEN 등급(관리자): 모든 채팅방 접근 허용
     * - 그 외: 본인이 고객인 채팅방만 접근 허용
     */
    public void validateRoomAccess(ChatRoom room, Long requesterId) {
        Member requester = memberService.getMemberById(requesterId);
        boolean isAdmin = requester.getGrade() == Grade.GREEN;
        boolean isOwner = room.isCustomer(requesterId);
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    /**
     * 채팅방의 updatedAt을 현재 시각으로 갱신한다. 방 목록을 "최근 대화순"으로
     * 정렬하기 위한 용도로, 메시지 전송 시마다 호출된다.
     *
     * @param roomId 갱신할 채팅방 ID
     */
    @Transactional
    public void updateLastMessageTime(Long roomId) {
        chatRoomRepository.updateLastMessageTime(roomId);
    }

}
