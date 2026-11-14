package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 闲置物品实体
 */
@Data
@TableName("tb_item")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者ID */
    private Long sellerId;

    /** 分类ID */
    private Long categoryId;

    private String title;

    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 原价（划线价） */
    private BigDecimal originalPrice;

    /** 成色 1全新 2九成新 3八成新 4七成新 5六成新及以下 */
    private Integer conditionLevel;

    /** 封面图 */
    private String coverImg;

    /** 交易校区 */
    private String campus;

    /** 0待审核 1在售 2已下架 3已售出 4驳回 5交易中 */
    private Integer status;

    /** 审核驳回理由 */
    private String auditRemark;

    private Integer viewCount;

    private Integer favoriteCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /* ---------------- 以下为非数据库字段，供页面展示 ---------------- */

    @TableField(exist = false)
    private String sellerName;

    @TableField(exist = false)
    private String sellerAvatar;

    @TableField(exist = false)
    private String categoryName;

    /** 成色文案 */
    @TableField(exist = false)
    private String conditionText;

    /** 状态文案 */
    @TableField(exist = false)
    private String statusText;

    /** 当前登录用户是否已收藏 */
    @TableField(exist = false)
    private Boolean favorited = false;

    /** 图片列表 */
    @TableField(exist = false)
    private List<ItemImage> images = new ArrayList<>();
}