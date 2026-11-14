package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("tb_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** BCrypt 密文 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像相对路径 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱，接收站内通知邮件 */
    private String email;

    /** 学号 */
    private String studentNo;

    /** 校区 */
    private String campus;

    /** 信用分 */
    private Integer creditScore;

    /** 1 正常 0 禁用 */
    private Integer status;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 非数据库字段：角色编码集合，用于页面展示 */
    @TableField(exist = false)
    private String roleNames;
}