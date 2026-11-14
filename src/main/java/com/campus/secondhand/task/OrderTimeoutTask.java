package com.campus.secondhand.task;

import com.campus.secondhand.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时兜底任务：每 5 分钟扫描一次超时未支付订单
 *
 * <p>RabbitMQ 延迟队列负责准点关闭，本任务是 MQ 不可用或消息丢失时的补偿手段，
 * 两者共用 OrderService.autoCancel 的 CAS 状态机，重复执行不会出错。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    /** 支付时限（分钟），与 MQ 延迟时间保持一致 */
    @Value("${app.order-timeout-minutes:30}")
    private int timeoutMinutes;

    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        int count = orderService.scanTimeoutOrders(timeoutMinutes);
        if (count > 0) {
            log.info("定时任务关闭超时未支付订单 {} 笔", count);
        }
    }
}