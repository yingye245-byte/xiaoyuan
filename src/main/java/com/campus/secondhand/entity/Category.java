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
 * 物品分类实体，支持一级分类与二级分类
 */
@Data
@TableName("tb_category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级ID，0 为一级分类 */
    private Long parentId;

    private String name;

    private String icon;

    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    /** 非数据库字段：子分类 */
    @TableField(exist = false)
    private List<Category> children = new ArrayList<>();

    /** 非数据库字段：该分类下物品数量 */
    @TableField(exist = false)
    private Integer itemCount;
}