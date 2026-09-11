package org.example.murderhelp.domain.chat.dto;

import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long roomId,
    Long memberId,
    String content,
    ChatMessageType messageType,
    LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getChatRoom().getId(),
            chatMessage.getMemberId(),
            chatMessage.getContent(),
            chatMessage.getMessageType(),
            chatMessage.getCreatedAt()
        );
    }
}
