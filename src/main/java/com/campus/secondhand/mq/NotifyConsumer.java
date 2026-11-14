package com.campus.secondhand.mq;

import com.campus.secondhand.config.RabbitMQConfig;
import com.campus.secondhand.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 站内消息消费者：订单/审核/留言状态变化异步落地为站内信
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyConsumer {

    private final MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFY)
    public void onMessage(NotifyMessage message) {
        log.info("收到站内消息推送：userId={} title={}", message.getUserId(), message.getTitle());
        messageService.save(message);
    }
}