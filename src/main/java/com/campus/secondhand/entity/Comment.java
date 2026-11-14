package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品留言实体，支持两级：一级留言与卖家回复
 */
@Data
@TableName("tb_comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private Long userId;

    /** 父留言ID，0 为一级留言 */
    private Long parentId;

    private String content;

    private LocalDateTime createTime;

    /* ---------------- 非数据库字段 ---------------- */

    @TableField(exist = false)
    private String userNickname;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String itemTitle;

    /** 一级留言下的回复列表 */
    @TableField(exist = false)
    private List<Comment> replies = new ArrayList<>();
}