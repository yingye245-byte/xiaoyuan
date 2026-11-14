package com.campus.secondhand.mq;

import com.campus.secondhand.config.RabbitMQConfig;
import com.campus.secondhand.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消消费者：延迟队列消息到期后触发，自动关闭未支付订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CANCEL)
    public void onMessage(OrderDelayMessage message) {
        log.info("收到订单超时消息：orderId={}", message.getOrderId());
        boolean canceled = orderService.autoCancel(message.getOrderId());
        if (canceled) {
            log.info("订单 {} 已被 MQ 延迟消息自动关闭", message.getOrderId());
        } else {
            log.info("订单 {} 无需关闭（已支付或已处理）", message.getOrderId());
        }
    }
}