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
 * 支付流水（担保交易）
 * <p>
 * 一笔订单只会有一条「支付成功」的流水；支付成功代表买家款项已进入平台托管，
 * 买家确认收货后订单完成，平台再与卖家结算，从而保证买卖双方资金安全。
 */
@Data
@TableName("tb_payment")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台支付流水号 */
    private String payNo;

    private Long orderId;

    /** 订单号快照，便于后台检索 */
    private String orderNo;

    private Long itemId;

    /** 物品标题快照，物品被删也不影响对账 */
    private String itemTitle;

    /** 付款人 */
    private Long userId;

    private BigDecimal amount;

    /** 支付渠道：ALIPAY / WECHAT / CAMPUS_CARD */
    private String channel;

    /** 0 待支付 1 支付成功（托管中） 2 支付失败 3 已关闭 4 已结算给卖家 */
    private Integer status;

    /** 模拟网关返回的交易号 */
    private String tradeNo;

    /** 网关异步通知时间 */
    private LocalDateTime notifyTime;

    /** 托管款结算给卖家的时间（买家确认收货或系统自动完成时写入） */
    private LocalDateTime settleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 非数据库字段：订单当前状态，用于后台对账展示 */
    @TableField(exist = false)
    private Integer orderStatus;

    @TableField(exist = false)
    private String buyerName;

    public String getChannelText() {
        return com.campus.secondhand.common.Constants.payChannelText(channel);
    }

    public String getStatusText() {
        return com.campus.secondhand.common.Constants.payStatusText(status);
    }

    /** 后台对账用：订单当前状态文案 */
    public String getOrderStatusText() {
        return com.campus.secondhand.common.Constants.orderStatusText(orderStatus);
    }

    /** 是否仍处于资金托管中（已支付但尚未结算给卖家） */
    public boolean isEscrow() {
        return status != null && status == com.campus.secondhand.common.Constants.PAY_SUCCESS;
    }
}