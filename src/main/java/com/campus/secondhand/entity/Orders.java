package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * 状态：0 待支付 / 1 已支付待交付 / 2 已完成 / 3 已取消（含超时关闭）
 */
@Data
@TableName("tb_orders")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    private Long itemId;

    private Long buyerId;

    private Long sellerId;

    /** 成交金额快照 */
    private BigDecimal amount;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime finishTime;

    private String cancelReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /* ---------------- 非数据库字段 ---------------- */

    @TableField(exist = false)
    private String itemTitle;

    @TableField(exist = false)
    private String itemCover;

    @TableField(exist = false)
    private String buyerName;

    @TableField(exist = false)
    private String sellerName;

    @TableField(exist = false)
    private String statusText;
}