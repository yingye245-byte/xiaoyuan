package com.campus.secondhand.service;

import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务：以 Redis Hash 存储 sc:cart:{userId}，field=物品 id、value=加入时间戳
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_SIZE = 20;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ItemMapper itemMapper;

    /**
     * 加入购物车：不能加自己的物品，只能加在售物品
     */
    public void add(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (item.getSellerId().equals(userId)) {
            throw new BusinessException("不能把自己发布的物品加入购物车");
        }
        if (item.getStatus() == null || item.getStatus() != Constants.ITEM_ON_SALE) {
            throw new BusinessException("该物品当前不可购买");
        }
        String key = RedisKeys.cart(userId);
        Long size = redisTemplate.opsForHash().size(key);
        if (size != null && size >= MAX_SIZE
                && !Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, itemId))) {
            throw new BusinessException("购物车最多放 " + MAX_SIZE + " 件物品");
        }
        redisTemplate.opsForHash().put(key, String.valueOf(itemId), String.valueOf(System.currentTimeMillis()));
        log.info("用户 {} 将物品 {} 加入购物车", userId, itemId);
    }

    public void remove(Long userId, Long itemId) {
        redisTemplate.opsForHash().delete(RedisKeys.cart(userId), String.valueOf(itemId));
    }

    public void clear(Long userId) {
        redisTemplate.delete(RedisKeys.cart(userId));
    }

    /**
     * 购物车明细：按加入时间倒序返回物品
     */
    public List<Item> list(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.cart(userId));
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = entries.keySet().stream()
                .map(k -> Long.valueOf(String.valueOf(k)))
                .sorted(Comparator.comparingLong((Long id) ->
                        Long.parseLong(String.valueOf(entries.get(String.valueOf(id))))).reversed())
                .toList();
        List<Item> items = itemMapper.selectBatchIds(ids);
        Map<Long, Item> map = new LinkedHashMap<>();
        items.forEach(i -> map.put(i.getId(), i));
        List<Item> result = new ArrayList<>();
        for (Long id : ids) {
            Item item = map.get(id);
            if (item != null) {
                item.setConditionText(Constants.conditionText(item.getConditionLevel()));
                item.setStatusText(Constants.itemStatusText(item.getStatus()));
                result.add(item);
            }
        }
        return result;
    }

    public int count(Long userId) {
        Long size = redisTemplate.opsForHash().size(RedisKeys.cart(userId));
        return size == null ? 0 : size.intValue();
    }

    /**
     * 购物车合计金额：只统计仍在售的物品，下架/交易中/已售出的不应计入（页面也不让结算）
     */
    public BigDecimal totalAmount(Long userId) {
        return list(userId).stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == Constants.ITEM_ON_SALE)
                .map(Item::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}