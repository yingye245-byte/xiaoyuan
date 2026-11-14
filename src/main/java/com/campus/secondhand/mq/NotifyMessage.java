package com.campus.secondhand.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 站内消息投递体：订单状态变化时推送给买家/卖家
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接收人 id */
    private Long userId;

    /** 消息类型：order / audit / system */
    private String type;

    private String title;

    private String content;

    /** 关联物品 id，前端可跳转 */
    private Long itemId;

    /** 关联订单 id */
    private Long orderId;
}