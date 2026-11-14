package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Message;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.MessageMapper;
import com.campus.secondhand.mq.NotifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 站内消息服务：MQ 消费者调用 save 落库，用户中心读取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    /** MQ 消费入口：把推送体落成一条站内消息 */
    public void save(NotifyMessage notify) {
        Message message = new Message();
        message.setUserId(notify.getUserId());
        message.setTitle(notify.getTitle());
        message.setContent(notify.getContent());
        message.setBizType(notify.getType() == null ? "SYSTEM" : notify.getType().toUpperCase());
        message.setBizId(notify.getOrderId() != null ? notify.getOrderId() : notify.getItemId());
        message.setIsRead(0);
        messageMapper.insert(message);
        log.info("站内消息已落库：userId={} title={}", notify.getUserId(), notify.getTitle());
    }

    public IPage<Message> page(Long userId, Integer pageNum, Integer pageSize) {
        return messageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getCreateTime));
    }

    public long unreadCount(Long userId) {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId).eq(Message::getIsRead, 0));
        return count == null ? 0 : count;
    }

    /** 标记单条已读，越权访问别人的消息直接拒绝 */
    public void markRead(Long id, Long userId) {
        Message message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该消息");
        }
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, id).set(Message::getIsRead, 1));
    }

    public void markAllRead(Long userId) {
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, userId).eq(Message::getIsRead, 0).set(Message::getIsRead, 1));
    }

    public void delete(Long id, Long userId) {
        Message message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该消息");
        }
        messageMapper.deleteById(id);
    }
}