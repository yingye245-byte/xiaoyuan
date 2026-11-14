package com.campus.secondhand.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单超时消息：投递到 TTL 队列，到期后经死信交换机进入取消队列
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDelayMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private LocalDateTime createTime;
}