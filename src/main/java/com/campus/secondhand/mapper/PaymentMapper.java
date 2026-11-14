package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付流水 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 后台支付流水分页：带订单状态与买家昵称
     */
    @Select("<script>" +
            "SELECT p.*, o.status AS order_status, u.nickname AS buyer_name " +
            "FROM tb_payment p " +
            "LEFT JOIN tb_orders o ON o.id = p.order_id " +
            "LEFT JOIN tb_user u ON u.id = p.user_id " +
            "WHERE 1 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.pay_no LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.order_no LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.item_title LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'>AND p.status = #{status} </if>" +
            "<if test='channel != null and channel != \"\"'>AND p.channel = #{channel} </if>" +
            "ORDER BY p.id DESC" +
            "</script>")
    IPage<Payment> selectPaymentPage(IPage<Payment> page,
                                     @Param("keyword") String keyword,
                                     @Param("status") Integer status,
                                     @Param("channel") String channel);

    /**
     * 统计托管中的资金总额（支付成功但订单未完成的金额）
     */
    @Select("SELECT IFNULL(SUM(p.amount), 0) FROM tb_payment p " +
            "JOIN tb_orders o ON o.id = p.order_id " +
            "WHERE p.status = 1 AND o.status IN (0, 1)")
    java.math.BigDecimal selectEscrowAmount();
}