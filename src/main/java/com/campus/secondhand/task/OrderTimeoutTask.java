package com.campus.secondhand.task;

import com.campus.secondhand.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时兜底任务
 *
 * <p>两件事：</p>
 * <ol>
 *   <li>每 5 分钟扫描超时未支付订单：RabbitMQ 延迟队列负责准点关闭，本任务是 MQ 不可用
 *       或消息丢失时的补偿手段，两者共用 OrderService.autoCancel 的 CAS 状态机，重复执行不会出错。</li>
 *   <li>每天凌晨扫描「已支付但买家长期未确认收货」的订单，自动完成并把托管款结算给卖家，
 *       避免担保资金永久挂账（配置 app.order-autofinish-days，默认 7 天）。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    /** 支付时限（分钟），与 MQ 延迟时间保持一致 */
    @Value("${app.order-timeout-minutes:30}")
    private int timeoutMinutes;

    /** 已支付订单允许买家确认收货的最长天数，超过后系统自动完成并结算 */
    @Value("${app.order-autofinish-days:7}")
    private int autofinishDays;

    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        int count = orderService.scanTimeoutOrders(timeoutMinutes);
        if (count > 0) {
            log.info("定时任务关闭超时未支付订单 {} 笔", count);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void autoFinishPaidOrders() {
        int count = orderService.autoFinishPaid(autofinishDays);
        if (count > 0) {
            log.info("定时任务自动完成超期未确认收货订单 {} 笔，托管款已结算", count);
        }
    }
}