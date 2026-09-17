package org.example.murderhelp.domain.chat.repository;

import org.example.murderhelp.domain.chat.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepositoryCustom {

    /**
     * lastMessageId보다 작은 id의 메시지를 최신순(id desc)으로 최대 limit건 조회한다.
     * lastMessageId가 null이면 해당 방의 가장 최근 메시지부터 조회한다.
     */
    List<ChatMessage> findMessagesByCursor(Long roomId, Long lastMessageId, int limit);
}
