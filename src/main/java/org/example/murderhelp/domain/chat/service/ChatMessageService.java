package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.bot.BotScenario;
import org.example.murderhelp.domain.chat.bot.dto.BotMessageDto;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.dto.CursorPageResponse;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;
import org.example.murderhelp.domain.chat.redis.ChatLastMessageCache;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.springframework.context.ApplicationEventPublisher;
import org.example.murderhelp.domain.chat.event.ChatMessageCreatedEvent;
import org.example.murderhelp.domain.chat.event.ChatRoomUpdatedEvent;
import org.example.murderhelp.domain.chat.bot.BotCommandDispatcher;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final MemberService memberService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final BotCommandDispatcher botCommandDispatcher;
    private final ChatLastMessageCache chatLastMessageCache;


    /**
     * 방 생성 직후 챗봇 환영 시나리오 메시지를 발송한다.
     *
     * @param roomId 환영 메시지를 보낼 채팅방 ID
     */
    @Transactional
    public void sendBotWelcomeMessage(Long roomId) {
        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        sendBotScenarioMessage(room, BotScenario.WELCOME);
    }

    /**
     * 상담 종료를 알리는 시스템 메시지를 저장하고, 해당 방의 마지막 메시지 캐시를 삭제한다
     * (COMPLETED 방은 목록에 마지막 메시지 미리보기를 노출하지 않기 위함).
     *
     * @param room 종료된 채팅방 엔티티
     */
    @Transactional
    public void sendCloseSystemMessage(ChatRoom room) {
        ChatMessage closeMsg = ChatMessage.builder()
                .chatRoom(room)
                .sender(memberService.getSystemBotMember())
                .content("[CLOSED] 상담이 완전히 종료되었습니다.")
                .messageType(ChatMessageType.SYSTEM)
                .build();

        // 종료 시스템 메시지 저장 후 Redis 캐시 삭제 (재기록 금지 — COMPLETED 방은 lastMessage 미노출)
        chatMessageRepository.save(closeMsg);
        deleteLastMessageFromRedis(room.getId());
        publishMessageEvent(room.getId(), closeMsg);
        publishRoomUpdatedEvent(room);
    }


    /**
     * 사용자가 보낸 메시지를 저장하고 실시간 브로드캐스트/캐시 갱신을 트리거한다.
     * 방 상태에 따라 전송 가능 여부를 검증하며, WAITING 상태에서 관리자가 첫 메시지를 보내면
     * 해당 관리자를 방에 배정한다. 봇 모드(BOT_MODE)에서 고객이 보낸 메시지는 이어서
     * BotCommandDispatcher로 위임된다.
     *
     * @param request 발신자(memberId)·방(roomId)·내용·메시지 타입을 담은 전송 요청
     * @throws BusinessException 현재 방 상태에서 전송이 허용되지 않는 경우
     */
    @Transactional
    public void sendMessage(ChatMessageSendRequest request) {
        ChatRoom room = chatRoomService.getRoomEntity(request.roomId());
        Member sender = memberService.getMemberById(request.memberId());
        boolean isCustomer = room.isCustomer(sender.getId());
        
        if (!room.getStatus().canSendMessage(isCustomer)) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }

        // 관리자 배정
        if (!isCustomer && room.getStatus() == ChatRoomStatus.WAITING && room.getAdmin() == null) {
            room.assignAdmin(sender);
        }

        // 일반 사용자(고객) 메시지
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.content())
                .messageType(request.messageType())
                .build();

        chatMessageRepository.save(message);
        updateLastMessageInRedis(room.getId(), message.getContent());
        publishMessageEvent(room.getId(), message);
        publishRoomUpdatedEvent(room);

        // 챗봇 메시지
        if (room.getStatus().isBotActive() && isCustomer) {
            botCommandDispatcher.execute(room, request.content(), this);
        }
        chatRoomService.updateLastMessageTime(room.getId());
    }


    /**
     * 시스템(봇 계정) 명의로 안내 메시지를 저장하고 발송한다.
     *
     * @param room    메시지를 보낼 채팅방
     * @param content 안내 문구
     */
    @Transactional
    public void sendSystemMessage(ChatRoom room, String content) {
        ChatMessage sysMsg = ChatMessage.builder()
                .chatRoom(room)
                .sender(memberService.getSystemBotMember())
                .content(content)
                .messageType(ChatMessageType.SYSTEM)
                .build();
        chatMessageRepository.save(sysMsg);
        updateLastMessageInRedis(room.getId(), sysMsg.getContent());
        publishMessageEvent(room.getId(), sysMsg);
        publishRoomUpdatedEvent(room);
    }

    /**
     * 미리 정의된 챗봇 시나리오(BotScenario)의 메시지를 발송한다.
     *
     * @param room     메시지를 보낼 채팅방
     * @param scenario 발송할 챗봇 시나리오
     */
    @Transactional
    public void sendBotScenarioMessage(ChatRoom room, BotScenario scenario) {
        sendBotMessage(room, scenario.getMessageDto());
    }

    /**
     * 챗봇 메시지(옵션/상품 목록 등 구조화된 데이터)를 JSON으로 직렬화해 BUTTON 타입으로 저장·발송한다.
     *
     * @param room       메시지를 보낼 채팅방
     * @param messageDto 직렬화할 챗봇 메시지 내용
     * @throws BusinessException 직렬화에 실패한 경우
     */
    @Transactional
    public void sendBotMessage(ChatRoom room, BotMessageDto messageDto) {
        try {
            String json = objectMapper.writeValueAsString(messageDto);
            ChatMessage botMsg = ChatMessage.builder()
                    .chatRoom(room)
                    .sender(memberService.getSystemBotMember())
                    .content(json)
                    .messageType(ChatMessageType.BUTTON)
                    .build();

            chatMessageRepository.save(botMsg);
            updateLastMessageInRedis(room.getId(), "[챗봇 메시지]");
            publishMessageEvent(room.getId(), botMsg);
            publishRoomUpdatedEvent(room);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateLastMessageInRedis(Long roomId, String content) {
        chatLastMessageCache.put(roomId, content);
    }

    private void deleteLastMessageFromRedis(Long roomId) {
        chatLastMessageCache.delete(roomId);
    }

    private void publishMessageEvent(Long roomId, ChatMessage message) {
        eventPublisher.publishEvent(ChatMessageCreatedEvent.of(roomId, message));
    }

    private void publishRoomUpdatedEvent(ChatRoom room) {
        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));
    }

    /**
     * 채팅방의 과거 메시지 내역을 커서(lastMessageId) 기반으로 조회한다. 호출 전 요청자의
     * 접근 권한을 검증한다. lastMessageId보다 작은 id의 메시지를 최신순으로 최대 size건 반환하며,
     * 실제로 더 오래된 메시지가 남아있는지(hasNext)와 다음 조회에 쓸 커서(nextCursorId)를 함께 내려준다.
     *
     * @param roomId        조회할 채팅방 ID
     * @param memberId      요청자 회원 ID
     * @param lastMessageId 이전 응답의 nextCursorId (최초 조회 시 null)
     * @param size          조회할 메시지 개수
     * @return 커서 기반 메시지 내역 페이지
     */
    public CursorPageResponse<ChatMessageResponse> getMessageHistory(Long roomId, Long memberId, Long lastMessageId, int size) {
        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        chatRoomService.validateRoomAccess(room, memberId);

        List<ChatMessage> messages = chatMessageRepository.findMessagesByCursor(roomId, lastMessageId, size + 1);

        boolean hasNext = messages.size() > size;
        List<ChatMessage> pageContent = hasNext ? messages.subList(0, size) : messages;
        Long nextCursorId = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;

        List<ChatMessageResponse> content = pageContent.stream()
                .map(ChatMessageResponse::from)
                .toList();

        return CursorPageResponse.of(content, hasNext, nextCursorId);
    }
}
