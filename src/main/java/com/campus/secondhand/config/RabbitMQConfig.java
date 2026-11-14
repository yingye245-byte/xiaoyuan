package com.campus.secondhand.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：一个业务 topic 交换机 + 一个死信交换机
 *
 * <pre>
 * sc.topic --sc.mail.send-->   sc.mail.queue
 *          --sc.notify.push--> sc.notify.queue
 *
 * sc.order.delay.queue（x-message-ttl = 订单超时时长）
 *        --死信--> sc.dlx --sc.order.cancel--> sc.order.cancel.queue
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String EXCHANGE_TOPIC = "sc.topic";
    public static final String EXCHANGE_DLX = "sc.dlx";

    public static final String QUEUE_ORDER_DELAY = "sc.order.delay.queue";
    public static final String QUEUE_ORDER_CANCEL = "sc.order.cancel.queue";
    public static final String QUEUE_MAIL = "sc.mail.queue";
    public static final String QUEUE_NOTIFY = "sc.notify.queue";

    public static final String RK_MAIL = "sc.mail.send";
    public static final String RK_NOTIFY = "sc.notify.push";
    public static final String RK_ORDER_CANCEL = "sc.order.cancel";

    /** 订单支付超时时长（毫秒），默认 30 分钟，演示时可调小 */
    @Value("${mq.order.delay-ms:1800000}")
    private long orderDelayMs;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_TOPIC, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    /** 延迟队列：消息到期后转入死信交换机，实现「超时未支付自动取消」 */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DELAY)
                .withArgument("x-message-ttl", orderDelayMs)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", RK_ORDER_CANCEL)
                .build();
    }

    /** 订单超时取消消费队列 */
    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CANCEL).build();
    }

    /** 异步邮件队列 */
    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(QUEUE_MAIL).build();
    }

    /** 站内消息推送队列 */
    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFY).build();
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(dlxExchange()).with(RK_ORDER_CANCEL);
    }

    @Bean
    public Binding mailBinding() {
        return BindingBuilder.bind(mailQueue()).to(topicExchange()).with(RK_MAIL);
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(topicExchange()).with(RK_NOTIFY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 自定义 RabbitTemplate：开启 mandatory 与 confirm 回调，消息不可达时能立刻发现
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息未到达交换机 cause={}", cause);
            }
        });
        template.setReturnsCallback(returned -> log.error("消息路由失败 exchange={} routingKey={} replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }
}