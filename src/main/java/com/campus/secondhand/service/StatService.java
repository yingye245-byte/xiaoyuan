package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.entity.DailyStat;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.DailyStatMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrdersMapper;
import com.campus.secondhand.mapper.StatMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 数据统计服务：后台看板 ECharts 数据源 + 每日统计快照
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {

    private final StatMapper statMapper;
    private final DailyStatMapper dailyStatMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final OrdersMapper ordersMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 看板概览指标：缓存 60 秒，防止答辩现场反复刷新打爆数据库
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> overview() {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.STAT_OVERVIEW);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }
        Map<String, Object> data = new HashMap<>(statMapper.selectOverview());
        redisTemplate.opsForValue().set(RedisKeys.STAT_OVERVIEW, data, 60, TimeUnit.SECONDS);
        return data;
    }

    /** 分类分布饼图 */
    public List<Map<String, Object>> categoryDistribution() {
        return statMapper.selectCategoryDistribution();
    }

    /** 近 7 天物品发布趋势 */
    public List<Map<String, Object>> itemTrend() {
        return statMapper.selectItemTrend();
    }

    /** 近 7 天订单趋势 */
    public List<Map<String, Object>> orderTrend() {
        return statMapper.selectOrderTrend();
    }

    /** 订单状态分布 */
    public List<Map<String, Object>> orderStatusDistribution() {
        return statMapper.selectOrderStatusDistribution();
    }

    /** 成交额 TOP5 卖家 */
    public List<Map<String, Object>> topSellers() {
        return statMapper.selectTopSellers();
    }

    /** 近 N 天日统计（折线图） */
    public List<DailyStat> recentDaily(int days) {
        List<DailyStat> list = new ArrayList<>(dailyStatMapper.selectRecent(days));
        java.util.Collections.reverse(list);
        return list;
    }

    /**
     * 生成当日统计快照（Spring Task 每日调用，也可手动触发）
     */
    public DailyStat recordDailyStat() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        DailyStat stat = dailyStatMapper.selectOne(new LambdaQueryWrapper<DailyStat>()
                .eq(DailyStat::getStatDate, today));
        if (stat == null) {
            stat = new DailyStat();
            stat.setStatDate(today);
        }
        Long newUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, start).lt(User::getCreateTime, end));
        Long newItems = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .ge(Item::getCreateTime, start).lt(Item::getCreateTime, end));
        Long orderCount = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end));
        Long cancelCount = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end)
                .eq(Orders::getStatus, com.campus.secondhand.common.Constants.ORDER_CANCELED));
        BigDecimal tradeAmount = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end)
                        .eq(Orders::getStatus, com.campus.secondhand.common.Constants.ORDER_FINISHED))
                .stream().map(Orders::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stat.setNewUserCount(newUsers == null ? 0 : newUsers.intValue());
        stat.setNewItemCount(newItems == null ? 0 : newItems.intValue());
        stat.setOrderCount(orderCount == null ? 0 : orderCount.intValue());
        stat.setCancelCount(cancelCount == null ? 0 : cancelCount.intValue());
        stat.setTradeAmount(tradeAmount);
        if (stat.getId() == null) {
            dailyStatMapper.insert(stat);
        } else {
            dailyStatMapper.updateById(stat);
        }
        redisTemplate.delete(RedisKeys.STAT_OVERVIEW);
        log.info("生成 {} 的平台日统计：新用户{} 新物品{} 订单{} 成交额{}",
                today, stat.getNewUserCount(), stat.getNewItemCount(),
                stat.getOrderCount(), stat.getTradeAmount());
        return stat;
    }
}