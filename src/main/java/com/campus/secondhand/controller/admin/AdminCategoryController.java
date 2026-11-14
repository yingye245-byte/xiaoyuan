package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 后台分类管理：新增、修改、删除、启停
 */
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final MenuService menuService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") Integer pageNum, Model model) {
        IPage<Category> page = categoryService.page(pageNum, 20);
        // 一级 / 二级分类树，带在售物品数量
        List<Category> all = categoryService.treeWithCount();
        model.addAttribute("page", page);
        model.addAttribute("topCategories", all);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "categories");
        return "admin/categories";
    }

    @PostMapping("/save")
    public String save(@RequestParam String name,
                       @RequestParam(defaultValue = "0") Long parentId,
                       @RequestParam(required = false) String icon,
                       @RequestParam(defaultValue = "0") Integer sort,
                       RedirectAttributes redirectAttributes) {
        try {
            Category category = new Category();
            category.setName(name);
            category.setParentId(parentId);
            category.setIcon(icon);
            category.setSort(sort);
            category.setStatus(1);
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("msg", "分类新增成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long id,
                         @RequestParam String name,
                         @RequestParam(defaultValue = "0") Long parentId,
                         @RequestParam(required = false) String icon,
                         @RequestParam(defaultValue = "0") Integer sort,
                         RedirectAttributes redirectAttributes) {
        try {
            Category category = new Category();
            category.setId(id);
            category.setName(name);
            category.setParentId(parentId);
            category.setIcon(icon);
            category.setSort(sort);
            categoryService.update(category);
            redirectAttributes.addFlashAttribute("msg", "分类修改成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("msg", "分类已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/toggle")
    public String toggle(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("msg", "分类状态已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}