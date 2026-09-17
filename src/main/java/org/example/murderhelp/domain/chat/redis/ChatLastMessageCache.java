package org.example.murderhelp.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "chat_last_messages" Redis 접근을 이 클래스 하나로 캡슐화한다.
 */
@Component
@RequiredArgsConstructor
public class ChatLastMessageCache {

    private static final String KEY = "chat_last_messages";

    private final StringRedisTemplate redisTemplate;

    public void put(Long roomId, String content) {
        redisTemplate.opsForHash().put(KEY, roomId.toString(), content);
    }

    public void delete(Long roomId) {
        redisTemplate.opsForHash().delete(KEY, roomId.toString());
    }

    public String get(Long roomId) {
        Object raw = redisTemplate.opsForHash().get(KEY, roomId.toString());
        return raw != null ? raw.toString() : null;
    }

    public Map<String, String> multiGet(List<String> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        List<Object> values = redisTemplate.opsForHash().multiGet(KEY, (List<Object>) (List<?>) roomIds);

        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < roomIds.size(); i++) {
            Object value = values.get(i);
            if (value != null) {
                result.put(roomIds.get(i), (String) value);
            }
        }
        return result;
    }

    public long size() {
        Long count = redisTemplate.opsForHash().size(KEY);
        return count != null ? count : 0L;
    }
}
