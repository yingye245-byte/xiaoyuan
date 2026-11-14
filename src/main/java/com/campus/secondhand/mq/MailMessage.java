package com.campus.secondhand.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 邮件消息体：由业务线程投递、消费者异步渲染模板并发送
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收件人邮箱 */
    private String to;

    /** 收件人昵称 */
    private String nickname;

    /** 邮件主题 */
    private String subject;

    /** 模板名，对应 templates/mail/{template}.html */
    private String template;

    /** 模板变量 */
    private Map<String, Object> params;

    /** 关联业务 id（用户 id / 订单 id），用于日志追溯 */
    private Long bizId;
}