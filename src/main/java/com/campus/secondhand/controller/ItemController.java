package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.dto.ItemPublishDTO;
import com.campus.secondhand.dto.ItemQueryDTO;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.service.CommentService;
import com.campus.secondhand.service.HistoryService;
import com.campus.secondhand.service.ItemService;
import com.campus.secondhand.service.StatService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * 物品门户控制器：首页、检索、详情、发布、编辑、下架
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final StatService statService;
    private final CommentService commentService;
    private final HistoryService historyService;

    /**
     * 首页：分类导航 + 热门推荐 + 最新发布
     */
    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("categories", categoryService.listTop());
        model.addAttribute("hotItems", itemService.hotItems(8));
        ItemQueryDTO query = new ItemQueryDTO();
        query.setPageSize(8);
        IPage<Item> latest = itemService.page(query);
        model.addAttribute("latestItems", latest.getRecords());
        model.addAttribute("overview", statService.overview());
        return "portal/index";
    }

    /**
     * 物品列表：分类、关键词、价格区间、校区、排序、分页
     */
    @GetMapping("/items")
    public String list(@ModelAttribute("query") ItemQueryDTO query, Model model) {
        query.setStatus(com.campus.secondhand.common.Constants.ITEM_ON_SALE);
        IPage<Item> page = itemService.page(query);
        model.addAttribute("page", page);
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("currentCategory", query.getCategoryId() == null
                ? null : categoryService.getById(query.getCategoryId()));
        return "portal/item-list";
    }

    /**
     * 物品详情：图片、卖家信息、留言，并记录浏览历史
     */
    @GetMapping("/item/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Long userId = SecurityUtils.getUserIdOrNull();
        Item item = itemService.detail(id, userId);
        model.addAttribute("item", item);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("comments", commentService.listByItem(id));
        model.addAttribute("recentItems", historyService.recent(userId, 4));
        historyService.record(userId, id);
        return "portal/item-detail";
    }

    /**
     * 发布页
     */
    @GetMapping("/items/publish")
    public String publishPage(Model model) {
        model.addAttribute("categories", categoryService.listForSelect());
        model.addAttribute("conditions", com.campus.secondhand.common.Constants.CONDITION_LEVELS);
        model.addAttribute("itemDTO", new ItemPublishDTO());
        return "portal/item-publish";
    }

    /**
     * 提交发布
     */
    @PostMapping("/items/publish")
    public String publish(@Valid @ModelAttribute("itemDTO") ItemPublishDTO dto,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("categories", categoryService.listForSelect());
            model.addAttribute("conditions", com.campus.secondhand.common.Constants.CONDITION_LEVELS);
            return "portal/item-publish";
        }
        try {
            itemService.publish(dto, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", "发布成功，等待管理员审核通过后自动上架");
            return "redirect:/my-items";
        } catch (Exception e) {
            log.warn("发布物品失败：{}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.listForSelect());
            model.addAttribute("conditions", com.campus.secondhand.common.Constants.CONDITION_LEVELS);
            return "portal/item-publish";
        }
    }

    /**
     * 编辑页：只能编辑自己的物品
     */
    @GetMapping("/items/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        Item item = itemService.getOwnItem(id, SecurityUtils.getUserId());
        ItemPublishDTO dto = new ItemPublishDTO();
        dto.setId(item.getId());
        dto.setCategoryId(item.getCategoryId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setOriginalPrice(item.getOriginalPrice());
        dto.setConditionLevel(item.getConditionLevel());
        dto.setCampus(item.getCampus());
        model.addAttribute("itemDTO", dto);
        model.addAttribute("item", item);
        model.addAttribute("categories", categoryService.listForSelect());
        model.addAttribute("conditions", com.campus.secondhand.common.Constants.CONDITION_LEVELS);
        return "portal/item-edit";
    }

    @PostMapping("/items/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("itemDTO") ItemPublishDTO dto,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        dto.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("item", itemService.getById(id));
            model.addAttribute("categories", categoryService.listForSelect());
            model.addAttribute("conditions", com.campus.secondhand.common.Constants.CONDITION_LEVELS);
            return "portal/item-edit";
        }
        try {
            itemService.update(dto, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", "修改成功，已重新提交审核");
            return "redirect:/my-items";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("item", itemService.getById(id));
            model.addAttribute("categories", categoryService.listForSelect());
            model.addAttribute("conditions", com.campus.secondhand.common.Constants.CONDITION_LEVELS);
            return "portal/item-edit";
        }
    }

    @PostMapping("/items/{id}/offline")
    public String offline(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        itemService.offline(id, SecurityUtils.getUserId());
        redirectAttributes.addFlashAttribute("msg", "物品已下架");
        return "redirect:/my-items";
    }

    @PostMapping("/items/{id}/resubmit")
    public String resubmit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        itemService.resubmit(id, SecurityUtils.getUserId());
        redirectAttributes.addFlashAttribute("msg", "已重新提交审核");
        return "redirect:/my-items";
    }

    @PostMapping("/items/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        itemService.delete(id, SecurityUtils.getUserId());
        redirectAttributes.addFlashAttribute("msg", "物品已删除");
        return "redirect:/my-items";
    }

    /**
     * 我的发布
     */
    @GetMapping("/my-items")
    public String myItems(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "12") Integer pageSize,
                          @RequestParam(required = false) Integer status,
                          Model model) {
        IPage<Item> page = itemService.myItems(SecurityUtils.getUserId(), pageNum, pageSize, status);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        return "portal/my-items";
    }
}