package com.campus.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物品检索条件：分类 / 关键词 / 价格区间 / 校区 / 排序 / 分页
 */
@Data
public class ItemQueryDTO {

    private String keyword;

    private Long categoryId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String campus;

    /** new 最新 / priceAsc 价格升 / priceDesc 价格降 / hot 最热 */
    private String sort;

    /** 物品状态，后台审核列表使用 */
    private Integer status;

    /** 卖家视角筛选 */
    private Long sellerId;

    private Integer pageNum = 1;

    private Integer pageSize = 12;

    /** 是否存在有效筛选条件，用于页面显示「清空筛选」 */
    public boolean hasFilter() {
        return (keyword != null && !keyword.isEmpty())
                || categoryId != null || minPrice != null || maxPrice != null
                || (campus != null && !campus.isEmpty());
    }
}