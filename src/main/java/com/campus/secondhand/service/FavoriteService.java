package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.FavoriteMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收藏服务：切换收藏状态并同步物品收藏数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 切换收藏
     *
     * @return true 表示当前已收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (item.getSellerId().equals(userId)) {
            throw new BusinessException("不能收藏自己发布的物品");
        }
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getItemId, itemId));
        boolean favorited;
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            favorited = false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setItemId(itemId);
            try {
                favoriteMapper.insert(favorite);
            } catch (DuplicateKeyException e) {
                // 并发双击：唯一索引 uk_user_item 挡住了第二条，按「已收藏」处理，不要抛 500
                log.info("用户 {} 重复收藏物品 {}，已忽略", userId, itemId);
                syncFavoriteCount(itemId);
                evictItemCache(itemId);
                return true;
            }
            favorited = true;
        }
        syncFavoriteCount(itemId);
        evictItemCache(itemId);
        log.info("用户 {} {} 收藏物品 {}", userId, favorited ? "添加" : "取消", itemId);
        return favorited;
    }

    /**
     * 收藏数按明细表重新校准：并发场景下自增/自减会漂移（删除 0 行也减一），
     * 以 tb_favorite 的实际行数为准，保证页面上的收藏数永远与数据一致。
     */
    private void syncFavoriteCount(Long itemId) {
        Long real = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getItemId, itemId));
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, itemId)
                .set(Item::getFavoriteCount, real == null ? 0 : real.intValue()));
    }

    private void evictItemCache(Long itemId) {
        redisTemplate.delete(RedisKeys.itemDetail(itemId));
        // 首页热门卡片也带收藏数，一起失效
        redisTemplate.delete(RedisKeys.ITEM_HOT);
    }

    /**
     * 我的收藏列表
     */
    public List<Favorite> list(Long userId) {
        return favoriteMapper.selectWithItemByUserId(userId);
    }

    public long count(Long userId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId));
        return count == null ? 0 : count;
    }
}