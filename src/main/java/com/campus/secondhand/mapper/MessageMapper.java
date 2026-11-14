package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}