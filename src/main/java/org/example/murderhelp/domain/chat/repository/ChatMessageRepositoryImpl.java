package org.example.murderhelp.domain.chat.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.murderhelp.domain.chat.entity.QChatMessage.chatMessage;
import static org.example.murderhelp.domain.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessage> findMessagesByCursor(Long roomId, Long lastMessageId, int limit) {
        return queryFactory
                .selectFrom(chatMessage)
                .join(chatMessage.sender, member).fetchJoin()
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        ltLastMessageId(lastMessageId)
                )
                .orderBy(chatMessage.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression ltLastMessageId(Long lastMessageId) {
        return lastMessageId != null ? chatMessage.id.lt(lastMessageId) : null;
    }
}
