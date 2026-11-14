package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.dto.ItemQueryDTO;
import com.campus.secondhand.entity.AuditRecord;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.mapper.AuditRecordMapper;
import com.campus.secondhand.service.ItemService;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 后台物品管理：审核列表、审核通过/驳回、违规下架、审核记录
 */
@Controller
@RequestMapping("/admin/items")
@RequiredArgsConstructor
public class AdminItemController {

    private final ItemService itemService;
    private final MenuService menuService;
    private final AuditRecordMapper auditRecordMapper;

    /**
     * 物品审核列表：默认只看待审核
     */
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       Model model) {
        ItemQueryDTO query = new ItemQueryDTO();
        query.setKeyword(keyword);
        query.setStatus(status);
        query.setCategoryId(categoryId);
        query.setPageNum(pageNum);
        query.setPageSize(10);
        IPage<Item> page = itemService.page(query);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "items");
        return "admin/items";
    }

    /**
     * 审核通过 / 驳回
     */
    @PostMapping("/{id}/audit")
    public String audit(@PathVariable Long id,
                        @RequestParam boolean pass,
                        @RequestParam(required = false) String remark,
                        RedirectAttributes redirectAttributes) {
        try {
            itemService.audit(id, pass, remark, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", pass ? "已审核通过并上架" : "已驳回");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/items";
    }

    /**
     * 违规下架
     */
    @PostMapping("/{id}/offline")
    public String offline(@PathVariable Long id,
                          @RequestParam(required = false) String reason,
                          RedirectAttributes redirectAttributes) {
        try {
            itemService.forceOffline(id, reason);
            redirectAttributes.addFlashAttribute("msg", "物品已下架");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/items";
    }

    /**
     * 审核记录：完整留痕，答辩时可展示审核链路
     */
    @GetMapping("/audits")
    public String audits(@RequestParam(defaultValue = "1") Integer pageNum, Model model) {
        IPage<AuditRecord> page = auditRecordMapper.selectRecordPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, 10));
        model.addAttribute("page", page);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "audits");
        return "admin/audits";
    }
}