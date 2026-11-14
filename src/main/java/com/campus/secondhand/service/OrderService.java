package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrdersMapper;
import com.campus.secondhand.mq.MailMessage;
import com.campus.secondhand.mq.MqProducer;
import com.campus.secondhand.mq.NotifyMessage;
import com.campus.secondhand.mq.OrderDelayMessage;
import com.campus.secondhand.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务：下单（幂等 + 分布式锁 + CAS 状态机）、支付、确认、取消、超时关闭
 *
 * <pre>
 * 物品：在售(1) --下单--> 交易中(5) --支付--> 已售出(3)
 *                             |--取消/超时--> 在售(1)
 * 订单：待支付(0) --支付--> 待交付(1) --确认--> 已完成(2)
 *                |--取消/超时--> 已取消(3)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrdersMapper ordersMapper;
    private final ItemMapper itemMapper;
    private final UserService userService;
    private final ItemService itemService;
    private final MqProducer mqProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建订单：幂等令牌 + Redis 分布式锁 + 状态 CAS，三步缺一不可
     *
     * @param idemToken 页面渲染时下发的幂等令牌
     * @return 新订单 id
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long itemId, Long buyerId, String idemToken) {
        if (idemToken == null || idemToken.isBlank()) {
            throw new BusinessException("请求已过期，请刷新页面后重试");
        }
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.idemOrder(idemToken), String.valueOf(buyerId), 5, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException("订单正在提交中，请勿重复点击");
        }
        String lockKey = RedisKeys.itemLock(itemId);
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(buyerId), 10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("该物品正在被其他同学下单，请稍后再试");
        }
        try {
            Item item = itemMapper.selectById(itemId);
            if (item == null) {
                throw new BusinessException(404, "物品不存在");
            }
            if (item.getSellerId().equals(buyerId)) {
                throw new BusinessException("不能购买自己发布的物品");
            }
            int lockedRows = itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .eq(Item::getStatus, Constants.ITEM_ON_SALE)
                    .set(Item::getStatus, Constants.ITEM_LOCKED));
            if (lockedRows == 0) {
                throw new BusinessException("手慢了，该物品刚刚被其他同学下单");
            }
            Orders order = new Orders();
            order.setOrderNo(generateOrderNo(buyerId));
            order.setItemId(itemId);
            order.setBuyerId(buyerId);
            order.setSellerId(item.getSellerId());
            order.setAmount(item.getPrice());
            order.setStatus(Constants.ORDER_WAIT_PAY);
            ordersMapper.insert(order);
            itemService.evictItemCache(itemId);

            mqProducer.sendOrderDelay(new OrderDelayMessage(order.getId(), order.getCreateTime()));
            notifyParties(order, item, "订单已创建",
                    "买家已下单，请在 30 分钟内完成支付，超时订单将自动关闭。", "order-notify", true);
            log.info("订单创建成功：orderNo={} item={} buyer={}", order.getOrderNo(), itemId, buyerId);
            return order.getId();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 买家支付（模拟支付，无第三方网关）
     */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId, Long userId) {
        Orders order = getOwnOrder(orderId, userId, true);
        int rows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Constants.ORDER_WAIT_PAY)
                .set(Orders::getStatus, Constants.ORDER_PAID)
                .set(Orders::getPayTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, order.getItemId())
                .eq(Item::getStatus, Constants.ITEM_LOCKED)
                .set(Item::getStatus, Constants.ITEM_SOLD));
        itemService.evictItemCache(order.getItemId());
        Item item = itemMapper.selectById(order.getItemId());
        notifyParties(order, item, "买家已付款",
                "买家已完成支付，请尽快与买家约定时间地点完成线下交付。", "order-notify", false);
        log.info("订单 {} 支付成功", order.getOrderNo());
    }

    /**
     * 买家确认收货，订单完成
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId, Long userId) {
        Orders order = getOwnOrder(orderId, userId, true);
        int rows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Constants.ORDER_PAID)
                .set(Orders::getStatus, Constants.ORDER_FINISHED)
                .set(Orders::getFinishTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        Item item = itemMapper.selectById(order.getItemId());
        notifyParties(order, item, "交易已完成",
                "买家已确认收货，本次交易完成，感谢使用校园二手平台。", "order-notify", false);
        log.info("订单 {} 已完成", order.getOrderNo());
    }

    /**
     * 买家主动取消（仅待支付状态可取消）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId, String reason) {
        Orders order = getOwnOrder(orderId, userId, true);
        String cancelReason = (reason == null || reason.isBlank()) ? "买家主动取消" : reason;
        int rows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Constants.ORDER_WAIT_PAY)
                .set(Orders::getStatus, Constants.ORDER_CANCELED)
                .set(Orders::getCancelReason, cancelReason));
        if (rows == 0) {
            throw new BusinessException("只有待支付订单可以取消");
        }
        releaseItem(order.getItemId());
        Item item = itemMapper.selectById(order.getItemId());
        notifyParties(order, item, "订单已取消", "订单已取消，物品重新上架，可继续购买。",
                "order-notify", true);
        log.info("订单 {} 被买家取消：{}", order.getOrderNo(), cancelReason);
    }

    /**
     * 超时未支付自动取消：MQ 延迟队列消费者与定时任务共同调用，天然幂等
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean autoCancel(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !isStatus(order.getStatus(), Constants.ORDER_WAIT_PAY)) {
            return false;
        }
        int rows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Constants.ORDER_WAIT_PAY)
                .set(Orders::getStatus, Constants.ORDER_CANCELED)
                .set(Orders::getCancelReason, "超时未支付，系统自动关闭"));
        if (rows == 0) {
            return false;
        }
        releaseItem(order.getItemId());
        Item item = itemMapper.selectById(order.getItemId());
        notifyParties(order, item, "订单已超时关闭",
                "订单超过支付时限未支付，已自动关闭，物品重新上架。", "order-notify", true);
        log.info("订单 {} 超时未支付，已自动关闭", order.getOrderNo());
        return true;
    }

    /**
     * 扫描超时未支付订单（MQ 补偿兜底，定时任务调用）
     */
    public int scanTimeoutOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        var list = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Constants.ORDER_WAIT_PAY)
                .lt(Orders::getCreateTime, deadline)
                .last("LIMIT 50"));
        int count = 0;
        for (Orders order : list) {
            if (autoCancel(order.getId())) {
                count++;
            }
        }
        return count;
    }

    /** 我的购买 */
    public IPage<Orders> pageBuyer(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        IPage<Orders> page = ordersMapper.selectOrderPage(new Page<>(pageNum, pageSize), userId, null, status, null);
        page.getRecords().forEach(this::fillText);
        return page;
    }

    /** 我卖出的 */
    public IPage<Orders> pageSeller(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        IPage<Orders> page = ordersMapper.selectOrderPage(new Page<>(pageNum, pageSize), null, userId, status, null);
        page.getRecords().forEach(this::fillText);
        return page;
    }

    /** 后台订单分页 */
    public IPage<Orders> pageAdmin(Integer status, String keyword, Integer pageNum, Integer pageSize) {
        IPage<Orders> page = ordersMapper.selectOrderPage(new Page<>(pageNum, pageSize), null, null, status, keyword);
        page.getRecords().forEach(this::fillText);
        return page;
    }

    public Orders detail(Long orderId, Long userId) {
        Orders order = ordersMapper.selectOrderDetail(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        boolean admin = com.campus.secondhand.utils.SecurityUtils.isAdmin();
        if (!admin && !order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(403, "无权查看该订单");
        }
        fillText(order);
        return order;
    }

    /** 卖家发货提醒用：统计待交付数量 */
    public long countWaitDeliver(Long sellerId) {
        Long count = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getSellerId, sellerId).eq(Orders::getStatus, Constants.ORDER_PAID));
        return count == null ? 0 : count;
    }

    /**
     * 订单归属校验：买家视角校验 buyerId，卖家视角校验 sellerId
     */
    private Orders getOwnOrder(Long orderId, Long userId, boolean asBuyer) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        Long ownerId = asBuyer ? order.getBuyerId() : order.getSellerId();
        if (!ownerId.equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        return order;
    }

    /** 取消/关闭后物品回到在售状态 */
    private void releaseItem(Long itemId) {
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, itemId)
                .eq(Item::getStatus, Constants.ITEM_LOCKED)
                .set(Item::getStatus, Constants.ITEM_ON_SALE));
        itemService.evictItemCache(itemId);
    }

    /**
     * 买卖双方通知：站内消息必发，邮件按需（下单/取消发给买卖双方）
     */
    private void notifyParties(Orders order, Item item, String title, String content,
                               String mailTemplate, boolean mailBoth) {
        String itemTitle = item == null ? "相关物品" : item.getTitle();
        mqProducer.sendNotify(new NotifyMessage(order.getBuyerId(), "order", title, content,
                order.getItemId(), order.getId()));
        mqProducer.sendNotify(new NotifyMessage(order.getSellerId(), "order", title, content,
                order.getItemId(), order.getId()));
        User buyer = userService.getById(order.getBuyerId());
        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", order.getOrderNo());
        params.put("itemTitle", itemTitle);
        params.put("amount", order.getAmount());
        params.put("title", title);
        params.put("content", content);
        params.put("orderId", order.getId());
        mqProducer.sendMail(new MailMessage(buyer.getEmail(), buyer.getNickname(),
                "【校园二手】" + title, mailTemplate, params, order.getId()));
        if (mailBoth) {
            User seller = userService.getById(order.getSellerId());
            mqProducer.sendMail(new MailMessage(seller.getEmail(), seller.getNickname(),
                    "【校园二手】" + title, mailTemplate, params, order.getId()));
        }
    }

    private String generateOrderNo(Long buyerId) {
        String suffix = String.format("%04d", buyerId % 10000);
        String random = String.valueOf((int) (Math.random() * 900) + 100);
        return "SC" + LocalDateTime.now().format(ORDER_NO_FORMAT) + suffix + random;
    }

    private void fillText(Orders order) {
        order.setStatusText(Constants.orderStatusText(order.getStatus()));
    }

    private static boolean isStatus(Integer value, int target) {
        return value != null && value == target;
    }
}