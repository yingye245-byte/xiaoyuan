package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏实体
 */
@Data
@TableName("tb_favorite")
public class Favorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long itemId;

    private LocalDateTime createTime;

    /* ---------------- 非数据库字段：收藏列表展示物品快照 ---------------- */

    @TableField(exist = false)
    private String itemTitle;

    @TableField(exist = false)
    private String itemCover;

    @TableField(exist = false)
    private BigDecimal itemPrice;

    @TableField(exist = false)
    private Integer itemStatus;
}