package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.MailLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件日志 Mapper
 */
@Mapper
public interface MailLogMapper extends BaseMapper<MailLog> {
}