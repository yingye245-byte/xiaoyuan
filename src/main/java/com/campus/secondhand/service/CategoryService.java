package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.CategoryMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 分类服务：分类列表走 Redis 缓存（缓存穿透/击穿在方案中说明）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ItemMapper itemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 全部分类（一级分类 + 子分类，一级分类带「在售数量」），带 Redis 缓存
     */
    @SuppressWarnings("unchecked")
    public List<Category> listAll() {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.CATEGORY_ALL);
        if (cached instanceof List) {
            return (List<Category>) cached;
        }
        List<Category> list = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
        List<Category> roots = buildTree(list);
        fillItemCount(roots);
        redisTemplate.opsForValue().set(RedisKeys.CATEGORY_ALL, roots, 30, TimeUnit.MINUTES);
        log.info("分类缓存未命中，已从数据库加载 {} 个一级分类", roots.size());
        return roots;
    }

    /**
     * 填充每个一级分类的在售物品数量（包含其子分类下的物品）
     */
    private void fillItemCount(List<Category> roots) {
        Map<Long, Integer> counts = countOnSaleByCategory();
        for (Category root : roots) {
            int total = counts.getOrDefault(root.getId(), 0);
            if (root.getChildren() != null) {
                for (Category child : root.getChildren()) {
                    total += counts.getOrDefault(child.getId(), 0);
                }
            }
            root.setItemCount(total);
        }
    }

    private Map<Long, Integer> countOnSaleByCategory() {
        Map<Long, Integer> counts = new HashMap<>(16);
        for (Map<String, Object> row : categoryMapper.selectCountByCategory()) {
            Object id = row.get("category_id");
            Object cnt = row.get("cnt");
            if (id instanceof Number && cnt instanceof Number) {
                counts.put(((Number) id).longValue(), ((Number) cnt).intValue());
            }
        }
        return counts;
    }

    /**
     * 一级分类（首页/导航用）
     */
    public List<Category> listTop() {
        return listAll().stream().filter(c -> c.getParentId() == null || c.getParentId() == 0).toList();
    }

    /**
     * 后台分类分页
     */
    public IPage<Category> page(Integer pageNum, Integer pageSize) {
        return categoryMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort).orderByAsc(Category::getId));
    }

    public List<Category> listForSelect() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1).orderByAsc(Category::getSort).orderByAsc(Category::getId));
    }

    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    /**
     * 分类树（分类管理页展示）
     */
    public List<Category> tree() {
        return buildTree(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort).orderByAsc(Category::getId)));
    }

    /**
     * 分类树 + 在售物品数量（后台分类管理页使用，包含停用的分类）
     */
    public List<Category> treeWithCount() {
        List<Category> roots = tree();
        fillItemCount(roots);
        return roots;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Category category) {
        normalize(category);
        checkNameDuplicate(category);
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        categoryMapper.insert(category);
        evictCache();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Category category) {
        normalize(category);
        Category exist = getById(category.getId());
        if (category.getId().equals(category.getParentId())) {
            throw new BusinessException("分类不能将自己设为父级");
        }
        // 不能挂到自己的子孙下面，否则分类树出现环，前台分类导航整块消失且后台也修不回来
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        if (parentId != 0L && isDescendant(category.getId(), parentId)) {
            throw new BusinessException("不能把分类移动到它自己的子分类下面");
        }
        category.setParentId(parentId);
        checkNameDuplicate(category);
        categoryMapper.updateById(category);
        log.info("分类更新：{} -> {}", exist.getName(), category.getName());
        evictCache();
    }

    /**
     * 判断 candidate 是否是 ancestor 的子孙节点（沿 parent_id 一路向上找）
     */
    private boolean isDescendant(Long ancestorId, Long candidateId) {
        Long current = candidateId;
        int guard = 0;
        while (current != null && current != 0L && guard++ < 20) {
            if (current.equals(ancestorId)) {
                return true;
            }
            Category node = categoryMapper.selectById(current);
            current = node == null ? 0L : node.getParentId();
        }
        return false;
    }

    /**
     * 删除分类：分类下仍有物品时禁止删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getCategoryId, id));
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException("该分类下还有 " + itemCount + " 件物品，不能删除，可先停用");
        }
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("请先删除或迁移该分类下的子分类");
        }
        categoryMapper.deleteById(id);
        evictCache();
    }

    /**
     * 启用/停用分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        Category category = getById(id);
        int target = category.getStatus() != null && category.getStatus() == 1 ? 0 : 1;
        categoryMapper.update(null, new LambdaUpdateWrapper<Category>()
                .eq(Category::getId, id).set(Category::getStatus, target));
        evictCache();
    }

    public void evictCache() {
        redisTemplate.delete(RedisKeys.CATEGORY_ALL);
    }

    /**
     * 名称去空格并做长度校验：空名会让前台分类导航出现空白胶囊，必须拦在入库前
     */
    private void normalize(Category category) {
        String name = category.getName() == null ? "" : category.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (name.length() > 20) {
            throw new BusinessException("分类名称不能超过 20 个字");
        }
        category.setName(name);
        if (category.getIcon() != null) {
            category.setIcon(category.getIcon().trim());
        }
    }

    private void checkNameDuplicate(Category category) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, category.getName())
                .eq(Category::getParentId, category.getParentId() == null ? 0L : category.getParentId());
        if (category.getId() != null) {
            wrapper.ne(Category::getId, category.getId());
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("同级下已存在同名分类");
        }
    }

    private List<Category> buildTree(List<Category> all) {
        List<Category> roots = new ArrayList<>();
        for (Category category : all) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                roots.add(category);
            }
        }
        for (Category root : roots) {
            List<Category> children = all.stream()
                    .filter(c -> root.getId().equals(c.getParentId()))
                    .toList();
            root.setChildren(new ArrayList<>(children));
        }
        return roots;
    }
}