package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息实体，由 MQ 异步推送落地
 */
@Data
@TableName("tb_message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long userId;

    private String title;

    private String content;

    /** 业务类型 ORDER / ITEM / SYSTEM */
    private String bizType;

    private Long bizId;

    /** 0 未读 1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}