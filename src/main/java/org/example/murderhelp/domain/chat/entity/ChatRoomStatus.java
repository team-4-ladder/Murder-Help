package org.example.murderhelp.domain.chat.entity;

public enum ChatRoomStatus {
    BOT_MODE {
        @Override
        public boolean canSendMessage(boolean isCustomer) {
            // 봇 모드에서는 고객(Customer)만 메시지를 보낼 수 있음
            return isCustomer;
        }

        @Override
        public boolean isBotActive() {
            return true;
        }
    },
    WAITING,
    IN_PROGRESS,
    COMPLETED {
        @Override
        public boolean canSendMessage(boolean isCustomer) {
            // 종료된 방에서는 아무도 메시지를 보낼 수 없음
            return false;
        }

        @Override
        public boolean canClose() {
            // 이미 종료된 방은 다시 종료할 수 없음
            return false;
        }
    };

    public boolean canSendMessage(boolean isCustomer) {
        return true;
    }

    public boolean canClose() {
        return true;
    }

    public boolean isBotActive() {
        return false;
    }
}
