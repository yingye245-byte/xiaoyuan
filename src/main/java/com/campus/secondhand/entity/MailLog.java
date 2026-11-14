package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件发送日志实体：每一次异步邮件都留痕，便于答辩演示与排障
 */
@Data
@TableName("tb_mail_log")
public class MailLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toEmail;

    private String subject;

    /** COMMENT / ORDER / AUDIT / SYSTEM */
    private String bizType;

    /** 1 成功 0 失败 */
    private Integer status;

    private String errorMsg;

    private LocalDateTime createTime;
}