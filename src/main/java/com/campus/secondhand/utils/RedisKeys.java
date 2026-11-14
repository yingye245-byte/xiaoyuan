package com.campus.secondhand.utils;

/**
 * Redis Key 统一生成器：所有 key 必须以 sc: 开头，便于运维批量清理与监控
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static final String PREFIX = "sc:";

    /** 分类树缓存 */
    public static final String CATEGORY_ALL = PREFIX + "category:all";
    /** 首页热门物品缓存 */
    public static final String ITEM_HOT = PREFIX + "item:hot";
    /** 后台看板概览缓存 */
    public static final String STAT_OVERVIEW = PREFIX + "stat:overview";
    /** 邮件发送频率限制 */
    public static final String MAIL_LIMIT = PREFIX + "mail:limit:";

    /** 物品详情缓存 */
    public static String itemDetail(Long itemId) {
        return PREFIX + "item:detail:" + itemId;
    }

    /** 物品详情空值缓存，防穿透 */
    public static String itemNull(Long itemId) {
        return PREFIX + "item:null:" + itemId;
    }

    /** 下单分布式锁 */
    public static String itemLock(Long itemId) {
        return PREFIX + "lock:item:" + itemId;
    }

    /** 下单幂等令牌 */
    public static String idemOrder(String token) {
        return PREFIX + "idem:order:" + token;
    }

    /** 购物车 Hash */
    public static String cart(Long userId) {
        return PREFIX + "cart:" + userId;
    }

    /** 浏览记录 ZSet */
    public static String history(Long userId) {
        return PREFIX + "history:" + userId;
    }

    /** 物品浏览量计数器 */
    public static String itemViews(Long itemId) {
        return PREFIX + "item:views:" + itemId;
    }
}