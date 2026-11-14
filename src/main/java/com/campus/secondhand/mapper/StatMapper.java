package com.campus.secondhand.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 数据看板专用统计 Mapper：只做聚合计，避免把统计逻辑散落在 Service 里
 */
@Mapper
public interface StatMapper {

    /**
     * 各分类在售物品数量分布（饼图）
     */
    @Select("SELECT c.name AS name, COUNT(i.id) AS cnt FROM tb_category c " +
            "LEFT JOIN tb_item i ON i.category_id = c.id AND i.deleted = 0 " +
            "WHERE c.parent_id = 0 GROUP BY c.id, c.name ORDER BY cnt DESC")
    List<Map<String, Object>> selectCategoryDistribution();

    /**
     * 近 7 天每天新增物品数（折线图）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM tb_item WHERE deleted = 0 " +
            "AND create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY day ORDER BY day ASC")
    List<Map<String, Object>> selectItemTrend();

    /**
     * 近 7 天每天订单数（柱状图）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM tb_orders WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY day ORDER BY day ASC")
    List<Map<String, Object>> selectOrderTrend();

    /**
     * 订单状态分布（环形图）
     */
    @Select("SELECT status AS status, COUNT(*) AS cnt FROM tb_orders GROUP BY status")
    List<Map<String, Object>> selectOrderStatusDistribution();

    /**
     * 成交额 TOP5 卖家（横向条形图）
     */
    @Select("SELECT u.nickname AS name, SUM(o.amount) AS cnt FROM tb_orders o " +
            "INNER JOIN tb_user u ON u.id = o.seller_id " +
            "WHERE o.status = 2 GROUP BY u.id, u.nickname ORDER BY cnt DESC LIMIT 5")
    List<Map<String, Object>> selectTopSellers();

    /**
     * 平台汇总指标
     */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM tb_user WHERE deleted = 0) AS userCount, " +
            "(SELECT COUNT(*) FROM tb_item WHERE deleted = 0) AS itemCount, " +
            "(SELECT COUNT(*) FROM tb_item WHERE deleted = 0 AND status = 1) AS onSaleCount, " +
            "(SELECT COUNT(*) FROM tb_item WHERE deleted = 0 AND status = 0) AS waitAuditCount, " +
            "(SELECT COUNT(*) FROM tb_orders) AS orderCount, " +
            "(SELECT COUNT(*) FROM tb_orders WHERE status = 2) AS finishCount, " +
            "(SELECT IFNULL(SUM(amount), 0) FROM tb_orders WHERE status = 2) AS tradeAmount, " +
            "(SELECT COUNT(*) FROM tb_comment) AS commentCount, " +
            "(SELECT COUNT(*) FROM tb_message WHERE is_read = 0) AS unreadCount")
    Map<String, Object> selectOverview();
}