package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物品审核记录实体
 */
@Data
@TableName("tb_audit_record")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private Long adminId;

    /** 1 通过 2 驳回 3 违规下架 */
    private Integer result;

    private String remark;

    private LocalDateTime createTime;

    /* ---------------- 非数据库字段 ---------------- */

    @TableField(exist = false)
    private String adminName;

    @TableField(exist = false)
    private String itemTitle;
}