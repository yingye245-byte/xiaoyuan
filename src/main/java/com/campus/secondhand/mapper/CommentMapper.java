package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 留言 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 某件物品的全部留言（含留言人昵称头像），按父子顺序返回
     */
    @Select("SELECT c.*, u.nickname AS user_nickname, u.avatar AS user_avatar " +
            "FROM tb_comment c LEFT JOIN tb_user u ON u.id = c.user_id " +
            "WHERE c.item_id = #{itemId} " +
            "ORDER BY c.parent_id ASC, c.create_time ASC")
    List<Comment> selectByItemId(@Param("itemId") Long itemId);

    /**
     * 后台留言分页，支持按内容或物品标题搜索
     */
    @Select("<script>" +
            "SELECT c.*, u.nickname AS user_nickname, i.title AS item_title " +
            "FROM tb_comment c " +
            "LEFT JOIN tb_user u ON u.id = c.user_id " +
            "LEFT JOIN tb_item i ON i.id = c.item_id " +
            "<where>" +
            "<if test=\"keyword != null and keyword != ''\">" +
            " (c.content LIKE CONCAT('%', #{keyword}, '%') OR i.title LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            " ORDER BY c.create_time DESC" +
            "</script>")
    IPage<Comment> selectCommentPage(IPage<Comment> page, @Param("keyword") String keyword);
}