package com.campus.secondhand.mq;

import com.campus.secondhand.config.RabbitMQConfig;
import com.campus.secondhand.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 邮件消费者：在独立线程渲染模板并发送，主链路不做邮件 IO
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailConsumer {

    private final MailService mailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MAIL)
    public void onMessage(MailMessage message) {
        log.info("收到邮件消息：to={} subject={}", message.getTo(), message.getSubject());
        mailService.send(message);
    }
}