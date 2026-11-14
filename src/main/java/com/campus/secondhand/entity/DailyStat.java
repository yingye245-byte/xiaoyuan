package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 平台日统计实体，由 Spring Task 每日 00:05 汇总产出
 */
@Data
@TableName("tb_daily_stat")
public class DailyStat implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private Integer newUserCount;

    private Integer newItemCount;

    private Integer orderCount;

    private BigDecimal tradeAmount;

    private Integer cancelCount;

    private LocalDateTime createTime;
}