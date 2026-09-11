package org.example.murderhelp.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.global.entity.BaseTimeEntity;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms")
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // TODO: 인증/Member 도입 시 연관관계(ManyToOne 등) 매핑으로 변경 고려
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    // TODO: 인증/Member 도입 시 연관관계(ManyToOne 등) 매핑으로 변경 고려
    @Column(name = "admin_id")
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Builder
    public ChatRoom(String title, Long customerId) {
        this.title = title;
        this.customerId = customerId;
        this.status = ChatRoomStatus.BOT_MODE; // 기본값: 챗봇 모드
    }

    public void changeToWaiting() {
        this.status = ChatRoomStatus.WAITING;
    }

    public void assignAdmin(Long adminId) {
        this.adminId = adminId;
        this.status = ChatRoomStatus.IN_PROGRESS;
    }

    public void closeRoom() {
        if (!this.status.canClose()) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }
        this.status = ChatRoomStatus.COMPLETED;
    }

    public boolean isCustomer(Long memberId) {
        return this.customerId.equals(memberId);
    }
}
