package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 分类 Mapper
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 按分类统计在售物品数量（只统计 status=1 且未删除的物品）。
     * 返回 Map 的键就是 SQL 里的列名：category_id / cnt，
     * 不依赖实体字段映射，避免 COUNT 别名与驼峰映射之间的歧义。
     */
    @Select("SELECT category_id, COUNT(*) AS cnt FROM tb_item " +
            "WHERE status = 1 AND deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> selectCountByCategory();
}