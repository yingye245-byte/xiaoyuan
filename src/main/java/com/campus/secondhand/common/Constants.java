package com.campus.secondhand.common;

/**
 * 业务常量：角色、状态、Redis Key 前缀统一在此维护，禁止在业务代码里硬编码魔法值
 */
public final class Constants {

    private Constants() {
    }

    /* ---------------- 角色 ---------------- */
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_SELLER = "ROLE_SELLER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /* ---------------- 通用状态 ---------------- */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    /* ---------------- 物品状态（与建表脚本注释保持一致） ---------------- */
    /** 待审核 */
    public static final int ITEM_WAIT_AUDIT = 0;
    /** 在售 */
    public static final int ITEM_ON_SALE = 1;
    /** 已下架（卖家主动） */
    public static final int ITEM_OFFLINE = 2;
    /** 已售出 */
    public static final int ITEM_SOLD = 3;
    /** 审核不通过 */
    public static final int ITEM_AUDIT_REJECT = 4;
    /** 交易中：下单后被锁定，等待支付，超时或取消后回退为在售 */
    public static final int ITEM_LOCKED = 5;

    /* ---------------- 订单状态 ---------------- */
    /** 待支付 */
    public static final int ORDER_WAIT_PAY = 0;
    /** 已支付待交付 */
    public static final int ORDER_PAID = 1;
    /** 已完成 */
    public static final int ORDER_FINISHED = 2;
    /** 已取消（买家取消或超时关闭，原因写入 cancel_reason） */
    public static final int ORDER_CANCELED = 3;

    /* ---------------- 审核状态 ---------------- */
    public static final int AUDIT_WAIT = 0;
    public static final int AUDIT_PASS = 1;
    public static final int AUDIT_REJECT = 2;

    /* ---------------- 支付流水状态 ---------------- */
    /** 待支付 */
    public static final int PAY_WAIT = 0;
    /** 支付成功（资金进入平台托管） */
    public static final int PAY_SUCCESS = 1;
    /** 支付失败 */
    public static final int PAY_FAIL = 2;
    /** 已关闭（订单取消或超时关闭） */
    public static final int PAY_CLOSED = 3;
    /** 已结算（买家确认收货，托管款结算给卖家） */
    public static final int PAY_SETTLED = 4;

    /* ---------------- 支付渠道（演示环境均为模拟网关） ---------------- */
    public static final String PAY_CHANNEL_ALIPAY = "ALIPAY";
    public static final String PAY_CHANNEL_WECHAT = "WECHAT";
    public static final String PAY_CHANNEL_CAMPUS = "CAMPUS_CARD";

    /** 收银台展示顺序：code、名称、说明 */
    public static final String[][] PAY_CHANNELS = {
            {PAY_CHANNEL_ALIPAY, "支付宝", "模拟网关，演示环境不会真实扣款"},
            {PAY_CHANNEL_WECHAT, "微信支付", "模拟网关，演示环境不会真实扣款"},
            {PAY_CHANNEL_CAMPUS, "校园一卡通", "模拟网关，演示环境不会真实扣款"}
    };

    public static String payChannelText(String channel) {
        if (channel == null) {
            return "未知";
        }
        for (String[] item : PAY_CHANNELS) {
            if (item[0].equals(channel)) {
                return item[1];
            }
        }
        return channel;
    }

    public static String payStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case PAY_WAIT:
                return "待支付";
            case PAY_SUCCESS:
                return "支付成功（托管中）";
            case PAY_FAIL:
                return "支付失败";
            case PAY_CLOSED:
                return "已关闭";
            case PAY_SETTLED:
                return "已结算给卖家";
            default:
                return "未知";
        }
    }

    /* ---------------- 消息状态 ---------------- */
    public static final int MSG_UNREAD = 0;
    public static final int MSG_READ = 1;

    /* ---------------- 邮件发送状态 ---------------- */
    public static final int MAIL_SENDING = 0;
    public static final int MAIL_SUCCESS = 1;
    public static final int MAIL_FAIL = 2;

    /* ---------------- 物品成色 ---------------- */
    public static final String[] CONDITION_LEVELS = {"全新", "九成新", "八成新", "七成新", "六成新及以下"};

    public static String conditionText(Integer level) {
        if (level == null || level < 1 || level > CONDITION_LEVELS.length) {
            return "未填写";
        }
        return CONDITION_LEVELS[level - 1];
    }

    /* ---------------- 状态文案 ---------------- */
    public static String itemStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case ITEM_WAIT_AUDIT:
                return "待审核";
            case ITEM_ON_SALE:
                return "在售";
            case ITEM_OFFLINE:
                return "已下架";
            case ITEM_SOLD:
                return "已售出";
            case ITEM_AUDIT_REJECT:
                return "审核未通过";
            case ITEM_LOCKED:
                return "交易中";
            default:
                return "未知";
        }
    }

    public static String orderStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case ORDER_WAIT_PAY:
                return "待支付";
            case ORDER_PAID:
                return "已支付·待交付";
            case ORDER_FINISHED:
                return "已完成";
            case ORDER_CANCELED:
                return "已取消";
            default:
                return "未知";
        }
    }

    public static String auditStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case AUDIT_WAIT:
                return "待审核";
            case AUDIT_PASS:
                return "已通过";
            case AUDIT_REJECT:
                return "已驳回";
            default:
                return "未知";
        }
    }
}