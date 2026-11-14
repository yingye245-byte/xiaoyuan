package com.campus.secondhand.service;

import com.campus.secondhand.entity.Item;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 浏览记录服务：用 Redis ZSet 保存用户最近浏览的 20 件物品
 */
@Service
@RequiredArgsConstructor
public class HistoryService {

    private static final int MAX_HISTORY = 20;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ItemMapper itemMapper;

    /** 记录一次浏览，按时间倒序裁剪 */
    public void record(Long userId, Long itemId) {
        if (userId == null) {
            return;
        }
        String key = RedisKeys.history(userId);
        redisTemplate.opsForZSet().add(key, String.valueOf(itemId), System.currentTimeMillis());
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size != null && size > MAX_HISTORY) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_HISTORY - 1);
        }
    }

    /** 最近浏览（时间倒序） */
    public List<Item> recent(Long userId, int limit) {
        if (userId == null) {
            return new ArrayList<>();
        }
        Set<Object> ids = redisTemplate.opsForZSet()
                .reverseRange(RedisKeys.history(userId), 0, limit - 1L);
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> itemIds = ids.stream().map(id -> Long.valueOf(String.valueOf(id))).toList();
        List<Item> items = itemMapper.selectBatchIds(itemIds);
        List<Item> ordered = new ArrayList<>();
        for (Long id : itemIds) {
            items.stream().filter(i -> i.getId().equals(id)).findFirst().ifPresent(ordered::add);
        }
        return ordered;
    }

    public void clear(Long userId) {
        redisTemplate.delete(RedisKeys.history(userId));
    }
}