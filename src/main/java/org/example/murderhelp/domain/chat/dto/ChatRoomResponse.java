package org.example.murderhelp.domain.chat.dto;

import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;

import java.time.LocalDateTime;

public record ChatRoomResponse(
    Long roomId,
    String title,
    ChatRoomStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String lastMessage,
    String customerProfileImageUrl,
    String customerName,
    String customerEmail,
    String customerGrade
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return from(chatRoom, null);
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, String lastMessage) {
        return new ChatRoomResponse(
            chatRoom.getId(),
            chatRoom.getTitle(),
            chatRoom.getStatus(),
            chatRoom.getCreatedAt(),
            chatRoom.getUpdatedAt(),
            lastMessage,
            chatRoom.getCustomer() != null ? chatRoom.getCustomer().getProfileImageUrl() : null,
            chatRoom.getCustomer() != null ? chatRoom.getCustomer().getName() : null,
            chatRoom.getCustomer() != null ? chatRoom.getCustomer().getEmail() : null,
            chatRoom.getCustomer() != null ? chatRoom.getCustomer().getGrade().name() : null
        );
    }
}
