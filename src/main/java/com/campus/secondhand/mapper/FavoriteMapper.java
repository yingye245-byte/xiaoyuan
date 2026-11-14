package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏 Mapper
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 我的收藏列表（带物品快照信息）
     */
    @Select("SELECT f.id, f.user_id, f.item_id, f.create_time, " +
            "i.title AS item_title, i.cover_img AS item_cover, i.price AS item_price, i.status AS item_status " +
            "FROM tb_favorite f INNER JOIN tb_item i ON i.id = f.item_id AND i.deleted = 0 " +
            "WHERE f.user_id = #{userId} ORDER BY f.create_time DESC")
    List<Favorite> selectWithItemByUserId(@Param("userId") Long userId);
}