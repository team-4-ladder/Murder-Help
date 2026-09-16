package org.example.murderhelp.domain.chat.repository;

import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    // 각 방의 마지막 메시지를 단일 쿼리로 조회 (Redis 캐시 복구용)
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.id IN (" +
           "SELECT MAX(cm2.id) FROM ChatMessage cm2 " +
           "WHERE cm2.chatRoom.id IN :roomIds " +
           "GROUP BY cm2.chatRoom.id)")
    List<ChatMessage> findLastMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);
}
