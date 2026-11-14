package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物品图片实体（一件物品多张图）
 */
@Data
@TableName("tb_item_image")
public class ItemImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    /** 图片相对路径 */
    private String url;

    /** 排序，0 为封面 */
    private Integer sort;

    private LocalDateTime createTime;
}