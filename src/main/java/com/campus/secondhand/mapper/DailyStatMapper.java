package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.DailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 日统计 Mapper
 */
@Mapper
public interface DailyStatMapper extends BaseMapper<DailyStat> {

    /**
     * 最近 N 天统计，用于 ECharts 折线图
     */
    @Select("SELECT * FROM tb_daily_stat ORDER BY stat_date DESC LIMIT #{days}")
    List<DailyStat> selectRecent(@Param("days") int days);
}