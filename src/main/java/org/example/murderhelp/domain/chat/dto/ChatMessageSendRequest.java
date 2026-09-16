package org.example.murderhelp.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;

public record ChatMessageSendRequest(
    @NotNull(message = "채팅방 ID는 필수입니다.")
    Long roomId,
    
    @NotNull(message = "발신자 ID는 필수입니다.")
    Long memberId,
    
    @NotBlank(message = "메시지 내용은 필수입니다.")
    String content,

    ChatMessageType messageType
) {}
