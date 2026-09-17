package org.example.murderhelp.domain.chat.dto;

import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    boolean hasNext,
    Long nextCursorId
) {
    public static <T> CursorPageResponse<T> of(List<T> content, boolean hasNext, Long nextCursorId) {
        return new CursorPageResponse<>(content, hasNext, nextCursorId);
    }
}
