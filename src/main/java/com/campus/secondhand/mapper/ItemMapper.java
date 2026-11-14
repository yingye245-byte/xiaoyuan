package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.dto.ItemQueryDTO;
import com.campus.secondhand.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 物品 Mapper：复杂检索走 XML，简单增删改走 MyBatis-Plus
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    /**
     * 物品分页检索：支持分类、关键词、价格区间、校区、状态、排序
     */
    IPage<Item> selectItemPage(IPage<Item> page, @Param("q") ItemQueryDTO query);

    /**
     * 物品详情（带分类名与卖家信息）
     */
    Item selectItemDetail(@Param("id") Long id);
}