package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    /**
     * 订单分页：买家、卖家、管理员三种视角共用，按条件裁剪
     */
    IPage<Orders> selectOrderPage(IPage<Orders> page,
                                  @Param("buyerId") Long buyerId,
                                  @Param("sellerId") Long sellerId,
                                  @Param("status") Integer status,
                                  @Param("keyword") String keyword);

    /**
     * 订单详情（带物品与买卖双方信息）
     */
    Orders selectOrderDetail(@Param("id") Long id);

    /**
     * 在事务内锁定订单行（SELECT ... FOR UPDATE）。
     * <p>
     * 支付、取消、超时关单三条链路都先锁订单行，再动物品与支付流水，
     * 保证加锁顺序统一为「订单 → 物品 → 流水」，避免 ABBA 死锁。
     */
    @Select("SELECT * FROM tb_orders WHERE id = #{id} FOR UPDATE")
    Orders selectForUpdate(@Param("id") Long id);
}