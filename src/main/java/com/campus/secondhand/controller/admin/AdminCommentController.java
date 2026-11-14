package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Comment;
import com.campus.secondhand.service.CommentService;
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
 * 后台留言管理：查看与清理违规留言
 */
@Controller
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;
    private final MenuService menuService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       Model model) {
        IPage<Comment> page = commentService.adminPage(keyword, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "comments");
        return "admin/comments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            commentService.delete(id, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", "留言已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/comments";
    }
}