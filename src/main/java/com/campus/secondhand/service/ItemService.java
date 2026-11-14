package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.dto.ItemPublishDTO;
import com.campus.secondhand.dto.ItemQueryDTO;
import com.campus.secondhand.entity.AuditRecord;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemImage;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.AuditRecordMapper;
import com.campus.secondhand.mapper.FavoriteMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrdersMapper;
import com.campus.secondhand.mq.MailMessage;
import com.campus.secondhand.mq.MqProducer;
import com.campus.secondhand.mq.NotifyMessage;
import com.campus.secondhand.utils.FileUtils;
import com.campus.secondhand.utils.RedisKeys;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 物品服务：发布/编辑/下架/检索/详情缓存/审核
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final int MAX_IMAGE = 5;

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final FavoriteMapper favoriteMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final OrdersMapper ordersMapper;
    private final FileUtils fileUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MqProducer mqProducer;
    private final UserService userService;

    /**
     * 物品分页检索（分类/关键词/价格区间/校区/排序）
     */
    public IPage<Item> page(ItemQueryDTO query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(12);
        }
        IPage<Item> page = itemMapper.selectItemPage(new Page<>(query.getPageNum(), query.getPageSize()), query);
        page.getRecords().forEach(this::fillText);
        return page;
    }

    /**
     * 物品详情：先查缓存（含空值标记防穿透），未命中回源数据库
     */
    public Item detail(Long id, Long currentUserId) {
        if (redisTemplate.hasKey(RedisKeys.itemNull(id))) {
            throw new BusinessException(404, "物品不存在或已下架");
        }
        Object cached = redisTemplate.opsForValue().get(RedisKeys.itemDetail(id));
        Item item;
        if (cached instanceof Item cachedItem) {
            item = cachedItem;
            log.debug("物品详情命中缓存：{}", id);
        } else {
            item = itemMapper.selectItemDetail(id);
            if (item == null) {
                redisTemplate.opsForValue().set(RedisKeys.itemNull(id), 1, 2, TimeUnit.MINUTES);
                throw new BusinessException(404, "物品不存在或已下架");
            }
            fillText(item);
            // 只缓存「在售」快照：待审核/下架/驳回的详情不能进缓存，否则审核通过后仍会读到旧状态
            if (isStatus(item.getStatus(), Constants.ITEM_ON_SALE)) {
                redisTemplate.opsForValue().set(RedisKeys.itemDetail(id), item, 10, TimeUnit.MINUTES);
            }
        }
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, id).orderByAsc(ItemImage::getSort));
        item.setImages(images);
        if (currentUserId != null) {
            Long favoriteCount = favoriteMapper.selectCount(new LambdaQueryWrapper<com.campus.secondhand.entity.Favorite>()
                    .eq(com.campus.secondhand.entity.Favorite::getUserId, currentUserId)
                    .eq(com.campus.secondhand.entity.Favorite::getItemId, id));
            item.setFavorited(favoriteCount != null && favoriteCount > 0);
        }
        // 未上架的物品（待审核/已下架/已售出/驳回）只对卖家本人和管理员可见，
        // 否则任何人遍历 id 就能看到未过审内容和管理员驳回理由。
        boolean owner = currentUserId != null && currentUserId.equals(item.getSellerId());
        if (!owner && !SecurityUtils.isAdmin() && !isStatus(item.getStatus(), Constants.ITEM_ON_SALE)) {
            throw new BusinessException(404, "物品不存在或已下架");
        }
        increaseView(id);
        return item;
    }

    /**
     * 浏览量：Redis 计数 + 每 20 次批量回写数据库，避免高频写表
     */
    private void increaseView(Long id) {
        Long delta = stringRedisTemplate.opsForValue().increment(RedisKeys.itemViews(id));
        if (delta != null && delta % 20 == 0) {
            flushViews(id);
        }
    }

    /**
     * 把 Redis 中的增量浏览量合并进数据库。
     * 用 getAndDelete 保证「取值 + 清零」原子，避免并发下重复累加或丢增量。
     */
    public void flushViews(Long id) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(RedisKeys.itemViews(id));
        long delta = 0L;
        try {
            if (value != null) {
                delta = Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            log.warn("物品 {} 的浏览计数格式异常：{}", id, value);
        }
        if (delta <= 0) {
            return;
        }
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, id)
                .setSql("view_count = view_count + " + delta));
        // 浏览量变了，详情缓存里的 viewCount 也要失效，否则页面显示旧数字
        redisTemplate.delete(RedisKeys.itemDetail(id));
        log.info("物品 {} 浏览量回写 {} 次", id, delta);
    }

    /**
     * 首页热门物品：Redis 缓存 5 分钟
     */
    @SuppressWarnings("unchecked")
    public List<Item> hotItems(int limit) {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.ITEM_HOT);
        if (cached instanceof List) {
            return (List<Item>) cached;
        }
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, Constants.ITEM_ON_SALE)
                .orderByDesc(Item::getViewCount)
                .orderByDesc(Item::getId)
                .last("LIMIT " + limit));
        items.forEach(this::fillText);
        redisTemplate.opsForValue().set(RedisKeys.ITEM_HOT, items, 5, TimeUnit.MINUTES);
        return items;
    }

    /**
     * 发布物品：图片落盘 + 状态置为待审核 + 自动升级卖家角色
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ItemPublishDTO dto, Long userId) {
        List<MultipartFile> images = dto.getImages() == null ? List.of() : dto.getImages();
        images = images.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (images.size() > MAX_IMAGE) {
            throw new BusinessException("最多上传 " + MAX_IMAGE + " 张图片");
        }
        if (images.isEmpty()) {
            throw new BusinessException("请至少上传一张物品图片");
        }
        List<String> paths = new ArrayList<>();
        for (MultipartFile file : images) {
            paths.add(fileUtils.save(file, "item"));
        }
        Item item = new Item();
        item.setSellerId(userId);
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setOriginalPrice(dto.getOriginalPrice());
        item.setConditionLevel(dto.getConditionLevel());
        item.setCampus(dto.getCampus());
        item.setCoverImg(paths.get(0));
        item.setStatus(Constants.ITEM_WAIT_AUDIT);
        item.setViewCount(0);
        item.setFavoriteCount(0);
        itemMapper.insert(item);
        saveImages(item.getId(), paths);
        userService.upgradeToSeller(userId);
        mqProducer.sendNotify(new NotifyMessage(userId, "item", "物品已提交审核",
                "你发布的《" + item.getTitle() + "》已提交审核，审核通过后自动上架。", item.getId(), null));
        log.info("用户 {} 发布物品成功：{}", userId, item.getTitle());
        return item.getId();
    }

    /**
     * 编辑物品：仅本人、且不是交易中/已售出时可编辑，编辑后重新进入待审核
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(ItemPublishDTO dto, Long userId) {
        Item exist = getOwnItem(dto.getId(), userId);
        if (isStatus(exist.getStatus(), Constants.ITEM_SOLD) || isStatus(exist.getStatus(), Constants.ITEM_LOCKED)) {
            throw new BusinessException("该物品已售出或正在交易，不能编辑");
        }
        List<String> keep = dto.getKeepImages() == null ? new ArrayList<>() : new ArrayList<>(dto.getKeepImages());
        // keepImages 由表单提交，必须校验确实属于该物品，否则可以写入任意外部 URL 当封面
        Set<String> ownedImages = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, exist.getId()))
                .stream().map(ItemImage::getUrl).collect(Collectors.toSet());
        keep.removeIf(url -> url == null || !ownedImages.contains(url));
        List<MultipartFile> newFiles = dto.getImages() == null ? List.of()
                : dto.getImages().stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (keep.size() + newFiles.size() > MAX_IMAGE) {
            throw new BusinessException("最多保留/上传 " + MAX_IMAGE + " 张图片");
        }
        if (keep.isEmpty() && newFiles.isEmpty()) {
            throw new BusinessException("请至少保留一张物品图片");
        }
        for (MultipartFile file : newFiles) {
            keep.add(fileUtils.save(file, "item"));
        }
        // 用 UpdateWrapper 显式 set：MP 的 updateById 默认忽略 null 字段，
        // 会导致编辑后旧审核驳回理由清不掉（页面一直显示上一条驳回原因）。
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, exist.getId())
                .set(Item::getCategoryId, dto.getCategoryId())
                .set(Item::getTitle, dto.getTitle())
                .set(Item::getDescription, dto.getDescription())
                .set(Item::getPrice, dto.getPrice())
                .set(Item::getOriginalPrice, dto.getOriginalPrice())
                .set(Item::getConditionLevel, dto.getConditionLevel())
                .set(Item::getCampus, dto.getCampus())
                .set(Item::getCoverImg, keep.get(0))
                .set(Item::getStatus, Constants.ITEM_WAIT_AUDIT)
                .set(Item::getAuditRemark, null)
                .set(Item::getUpdateTime, LocalDateTime.now()));

        itemImageMapper.delete(new LambdaQueryWrapper<ItemImage>().eq(ItemImage::getItemId, exist.getId()));
        saveImages(exist.getId(), keep);
        evictItemCache(exist.getId());
        log.info("物品 {} 被用户 {} 编辑，重新提交审核", exist.getId(), userId);
    }

    /**
     * 卖家下架自己的物品
     */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id, Long userId) {
        Item item = getOwnItem(id, userId);
        if (!isStatus(item.getStatus(), Constants.ITEM_ON_SALE)) {
            throw new BusinessException("只有在售物品才能下架");
        }
        // CAS：只有当前仍是「在售」才改成「已下架」，避免把并发进来的「交易中」覆盖掉
        int rows = itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, id)
                .eq(Item::getStatus, Constants.ITEM_ON_SALE)
                .set(Item::getStatus, Constants.ITEM_OFFLINE));
        if (rows == 0) {
            throw new BusinessException("物品状态已变化，请刷新后重试");
        }
        evictItemCache(id);
        log.info("物品 {} 已下架", id);
    }

    /**
     * 重新提交上架申请（下架/驳回状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long id, Long userId) {
        Item item = getOwnItem(id, userId);
        if (!isStatus(item.getStatus(), Constants.ITEM_OFFLINE)
                && !isStatus(item.getStatus(), Constants.ITEM_AUDIT_REJECT)) {
            throw new BusinessException("当前状态不能重新上架");
        }
        int rows = itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, id)
                .in(Item::getStatus, Constants.ITEM_OFFLINE, Constants.ITEM_AUDIT_REJECT)
                .set(Item::getStatus, Constants.ITEM_WAIT_AUDIT)
                .set(Item::getAuditRemark, null));
        if (rows == 0) {
            throw new BusinessException("物品状态已变化，请刷新后重试");
        }
        evictItemCache(id);
    }

    /**
     * 逻辑删除物品
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        Item item = getOwnItem(id, userId);
        if (isStatus(item.getStatus(), Constants.ITEM_LOCKED)) {
            throw new BusinessException("物品正在交易中，不能删除");
        }
        if (isStatus(item.getStatus(), Constants.ITEM_SOLD)) {
            throw new BusinessException("已售出的物品不能删除，历史订单需要保留该物品信息");
        }
        // 有任何订单引用（含已取消）都不允许删除，否则订单详情里物品标题/图片会变成空
        Long orderCount = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getItemId, id));
        if (orderCount != null && orderCount > 0) {
            throw new BusinessException("该物品已有订单记录，不能删除，可改为下架");
        }
        itemMapper.deleteById(id);
        evictItemCache(id);
        log.info("物品 {} 已被卖家删除", id);
    }

    /**
     * 管理员审核：通过上架、驳回记录理由，并推送站内消息与邮件
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean pass, String remark, Long adminId) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!isStatus(item.getStatus(), Constants.ITEM_WAIT_AUDIT)) {
            throw new BusinessException("该物品当前不在待审核状态");
        }
        if (!pass && (remark == null || remark.isBlank())) {
            throw new BusinessException("驳回时必须填写驳回理由");
        }
        int target = pass ? Constants.ITEM_ON_SALE : Constants.ITEM_AUDIT_REJECT;
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, id)
                .set(Item::getStatus, target)
                .set(Item::getAuditRemark, pass ? null : remark));
        AuditRecord record = new AuditRecord();
        record.setItemId(id);
        record.setAdminId(adminId);
        record.setResult(pass ? Constants.AUDIT_PASS : Constants.AUDIT_REJECT);
        record.setRemark(remark);
        auditRecordMapper.insert(record);
        evictItemCache(id);

        String title = pass ? "物品审核通过" : "物品审核未通过";
        String content = pass
                ? "你发布的《" + item.getTitle() + "》已通过审核并上架。"
                : "你发布的《" + item.getTitle() + "》未通过审核，理由：" + remark;
        mqProducer.sendNotify(new NotifyMessage(item.getSellerId(), "audit", title, content, id, null));
        var seller = userService.getById(item.getSellerId());
        Map<String, Object> params = new HashMap<>();
        params.put("itemTitle", item.getTitle());
        params.put("result", pass ? "通过" : "未通过");
        params.put("remark", remark == null ? "" : remark);
        params.put("itemId", id);
        mqProducer.sendMail(new MailMessage(seller.getEmail(), seller.getNickname(),
                title, "audit-result", params, id));
        log.info("管理员 {} 审核物品 {}，结果：{}", adminId, id, pass ? "通过" : "驳回");
    }

    /**
     * 后台违规下架：管理员直接把在售物品下架
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceOffline(Long id, String reason) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (isStatus(item.getStatus(), Constants.ITEM_LOCKED)) {
            throw new BusinessException("交易中的物品不能下架");
        }
        // 已售出的物品必须保持「已售出」，否则卖家重提后会二次成交，同一物品出现两张有效订单
        if (isStatus(item.getStatus(), Constants.ITEM_SOLD)) {
            throw new BusinessException("该物品已成交，不能下架");
        }
        int rows = itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, id)
                .eq(Item::getStatus, item.getStatus())
                .set(Item::getStatus, Constants.ITEM_OFFLINE)
                .set(Item::getAuditRemark, reason));
        if (rows == 0) {
            throw new BusinessException("物品状态已变化，请刷新后重试");
        }
        evictItemCache(id);
        mqProducer.sendNotify(new NotifyMessage(item.getSellerId(), "audit", "物品被管理员下架",
                "《" + item.getTitle() + "》已被管理员下架，原因：" + (reason == null ? "违规发布" : reason),
                id, null));
        log.info("管理员下架物品 {}：{}", id, reason);
    }

    /** 我的发布 */
    public IPage<Item> myItems(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        ItemQueryDTO query = new ItemQueryDTO();
        query.setSellerId(userId);
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return page(query);
    }

    /** 卖家自己的物品，校验归属 */
    public Item getOwnItem(Long id, Long userId) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!item.getSellerId().equals(userId)) {
            throw new BusinessException(403, "只能操作自己发布的物品");
        }
        return item;
    }

    public Item getById(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        fillText(item);
        // 编辑页要靠 images 渲染「保留原图」复选框，这里必须一起查出来
        item.setImages(itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, id).orderByAsc(ItemImage::getSort)));
        return item;
    }

    public long countByStatus(Integer status) {
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, status));
        return count == null ? 0 : count;
    }

    /** 清除物品相关缓存 */
    public void evictItemCache(Long id) {
        redisTemplate.delete(RedisKeys.itemDetail(id));
        redisTemplate.delete(RedisKeys.itemNull(id));
        redisTemplate.delete(RedisKeys.ITEM_HOT);
        // 首页分类的「在售 N 件」与统计卡都依赖物品状态，物品变更后必须一起失效
        redisTemplate.delete(RedisKeys.CATEGORY_ALL);
        redisTemplate.delete(RedisKeys.STAT_OVERVIEW);
    }

    private void saveImages(Long itemId, List<String> paths) {
        int sort = 0;
        for (String path : paths) {
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(path);
            image.setSort(sort++);
            itemImageMapper.insert(image);
        }
    }

    /** 待审核物品（后台首页提醒用） */
    public List<Item> pendingAudit(int limit) {
        ItemQueryDTO query = new ItemQueryDTO();
        query.setStatus(Constants.ITEM_WAIT_AUDIT);
        query.setPageSize(limit);
        return page(query).getRecords();
    }

    private void fillText(Item item) {
        item.setConditionText(Constants.conditionText(item.getConditionLevel()));
        item.setStatusText(Constants.itemStatusText(item.getStatus()));
    }

    /** 状态比较：常量是 int，这里统一做 null 安全判断，避免拆箱空指针 */
    private static boolean isStatus(Integer value, int target) {
        return value != null && value == target;
    }
}