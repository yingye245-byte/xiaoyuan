package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.dto.CommentDTO;
import com.campus.secondhand.entity.Comment;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.CommentMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mq.MailMessage;
import com.campus.secondhand.mq.MqProducer;
import com.campus.secondhand.mq.NotifyMessage;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 留言服务：物品详情页的咨询与回复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final MqProducer mqProducer;

    /**
     * 新增留言/回复：回复他人时通知被回复人，留言物品时通知卖家
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(CommentDTO dto, Long userId) {
        Item item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在或已删除");
        }
        Long parentId = dto.getParentId();
        Long targetUserId = item.getSellerId();
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getItemId().equals(dto.getItemId())) {
                throw new BusinessException("回复的留言不存在");
            }
            // 页面固定只渲染两级：回复「某条回复」时也挂到同一条主留言下，否则三级回复永远显示不出来
            parentId = (parent.getParentId() == null || parent.getParentId() == 0)
                    ? parent.getId() : parent.getParentId();
            targetUserId = parent.getUserId();
        }
        Comment comment = new Comment();
        comment.setItemId(dto.getItemId());
        comment.setUserId(userId);
        comment.setParentId(parentId == null ? 0L : parentId);
        comment.setContent(dto.getContent().trim());
        commentMapper.insert(comment);

        User author = userMapper.selectById(userId);
        String nickname = author == null ? "同学" : author.getNickname();
        Long notifyUserId = targetUserId;
        if (notifyUserId != null && !notifyUserId.equals(userId)) {
            mqProducer.sendNotify(new NotifyMessage(notifyUserId, "comment",
                    dto.getParentId() == null ? "收到新的物品留言" : "收到新的留言回复",
                    nickname + "：" + comment.getContent(), item.getId(), null));
        }
        if (notifyUserId != null && !notifyUserId.equals(userId)) {
            User target = userMapper.selectById(notifyUserId);
            if (target != null && target.getEmail() != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("nickname", target.getNickname());
                params.put("author", nickname);
                params.put("itemTitle", item.getTitle());
                params.put("content", comment.getContent());
                params.put("itemId", item.getId());
                mqProducer.sendMail(new MailMessage(target.getEmail(), target.getNickname(),
                        "你在校园二手平台的物品有新的留言", "comment-notify", params, item.getId()));
            }
        }
        log.info("用户 {} 在物品 {} 下留言", userId, item.getId());
    }

    /**
     * 物品详情页留言列表（两级：主留言 + 回复）
     */
    public List<Comment> listByItem(Long itemId) {
        List<Comment> all = commentMapper.selectByItemId(itemId);
        List<Comment> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
        for (Comment root : roots) {
            List<Comment> replies = all.stream()
                    .filter(c -> root.getId().equals(c.getParentId()))
                    .collect(Collectors.toList());
            root.setReplies(new ArrayList<>(replies));
        }
        return roots;
    }

    /**
     * 删除留言：本人或管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(404, "留言不存在");
        }
        boolean admin = SecurityUtils.isAdmin();
        if (!admin && !comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的留言");
        }
        // 连子回复一起删：先收集直接子级，再用 in 一次删掉（含历史三层数据的中间层）
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id))
                .forEach(child -> ids.add(child.getId()));
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .in(Comment::getId, ids)
                .or()
                .in(Comment::getParentId, ids));
        log.info("留言 {} 被用户 {} 删除", id, userId);
    }

    /**
     * 后台留言分页
     */
    public IPage<Comment> adminPage(String keyword, Integer pageNum, Integer pageSize) {
        return commentMapper.selectCommentPage(new Page<>(pageNum, pageSize), keyword);
    }

    public long countAll() {
        Long count = commentMapper.selectCount(null);
        return count == null ? 0 : count;
    }
}