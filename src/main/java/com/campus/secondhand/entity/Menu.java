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
 * 菜单/权限实体
 */
@Data
@TableName("tb_menu")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级ID，0 为顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 访问路径 */
    private String path;

    /** 权限标识，如 item:audit */
    private String perms;

    private String icon;

    /** 0 目录 1 菜单 2 按钮 */
    private Integer type;

    private Integer sort;

    /** 1 显示 0 隐藏 */
    private Integer visible;

    private LocalDateTime createTime;

    /** 非数据库字段：子菜单，用于后台侧边栏树形渲染 */
    @TableField(exist = false)
    private List<Menu> children = new ArrayList<>();
}