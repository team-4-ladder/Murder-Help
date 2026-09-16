package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.event.ChatRoomUpdatedEvent;
import org.example.murderhelp.domain.chat.repository.ChatRoomRepository;
import org.example.murderhelp.domain.member.entity.Grade;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final MemberService memberService;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

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

    @Transactional
    public void closeRoom(Long roomId) {
        ChatRoom room = getRoomEntity(roomId);
        room.closeRoom();

        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));
    }


    public Page<ChatRoomResponse> getRooms(Long customerId, ChatRoomStatus status, String keyword, Pageable pageable) {
        Page<ChatRoom> roomPage = chatRoomRepository.findRoomsByCondition(customerId, status, keyword, pageable);
        List<String> roomIds = roomPage.stream()
                                       .map(room -> room.getId().toString())
                                       .collect(Collectors.toList());

        Map<String, String> lastMsgMap = Map.of();
        if (!roomIds.isEmpty()) {
            List<Object> lastMessages = redisTemplate.opsForHash().multiGet("chat_last_messages", (List<Object>)(List<?>) roomIds);
            lastMsgMap = IntStream.range(0, roomIds.size())
                    .filter(i -> lastMessages.get(i) != null)
                    .boxed()
                    .collect(Collectors.toMap(roomIds::get, i -> (String) lastMessages.get(i)));
        }

        Map<String, String> finalLastMsgMap = lastMsgMap;
        return roomPage.map(room ->
                ChatRoomResponse.from(room, finalLastMsgMap.get(room.getId().toString())));
    }


    public ChatRoomResponse getRoom(Long roomId, Long memberId) {
        ChatRoom room = getRoomEntity(roomId);
        validateRoomAccess(room, memberId);
        return ChatRoomResponse.from(room);
    }

    public ChatRoom getRoomEntity(Long roomId) {
        return chatRoomRepository.findById(roomId)
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

    @Transactional
    public void updateLastMessageTime(Long roomId) {
        chatRoomRepository.updateLastMessageTime(roomId);
    }

}
