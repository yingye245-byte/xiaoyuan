package com.campus.secondhand.mq;

import com.campus.secondhand.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 统一的消息生产者：邮件、站内消息、订单超时延迟消息
 *
 * <p>MQ 不可用时只记录错误日志，不阻断主链路业务，
 * 订单超时取消另有 Spring Task 定时兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqProducer {

    private final RabbitTemplate rabbitTemplate;

    /** 异步发送邮件 */
    public void sendMail(MailMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TOPIC, RabbitMQConfig.RK_MAIL, message);
            log.info("邮件消息已投递：to={} subject={}", message.getTo(), message.getSubject());
        } catch (Exception e) {
            log.error("邮件消息投递失败：to={}", message.getTo(), e);
        }
    }

    /** 推送站内消息 */
    public void sendNotify(NotifyMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TOPIC, RabbitMQConfig.RK_NOTIFY, message);
            log.info("站内消息已投递：userId={} title={}", message.getUserId(), message.getTitle());
        } catch (Exception e) {
            log.error("站内消息投递失败：userId={}", message.getUserId(), e);
        }
    }

    /** 投递订单超时延迟消息（走默认交换机，路由键即队列名） */
    public void sendOrderDelay(OrderDelayMessage message) {
        try {
            rabbitTemplate.convertAndSend("", RabbitMQConfig.QUEUE_ORDER_DELAY, message);
            log.info("订单超时延迟消息已投递：orderId={}", message.getOrderId());
        } catch (Exception e) {
            log.error("订单超时延迟消息投递失败：orderId={}", message.getOrderId(), e);
        }
    }
}