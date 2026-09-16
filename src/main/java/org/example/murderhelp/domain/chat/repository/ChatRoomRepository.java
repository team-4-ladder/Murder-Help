package org.example.murderhelp.domain.chat.repository;

import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

    boolean existsByCustomerIdAndStatusIn(Long customerId, List<ChatRoomStatus> statuses);

    List<ChatRoom> findAllByStatusNot(ChatRoomStatus status);

    long countByStatusNot(ChatRoomStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :roomId")
    void updateLastMessageTime(@Param("roomId") Long roomId);

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.customer WHERE r.id = :roomId")
    Optional<ChatRoom> findByIdWithCustomer(@Param("roomId") Long roomId);
}
