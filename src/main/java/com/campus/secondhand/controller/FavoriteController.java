package com.campus.secondhand.controller;

import com.campus.secondhand.service.FavoriteService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 收藏控制器：切换收藏 + 我的收藏列表
 */
@Controller
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/favorite/{itemId}")
    public String toggle(@PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            boolean favorited = favoriteService.toggle(SecurityUtils.getUserId(), itemId);
            redirectAttributes.addFlashAttribute("msg", favorited ? "已加入收藏" : "已取消收藏");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/item/" + itemId;
    }

    @GetMapping("/user/favorites")
    public String list(Model model) {
        model.addAttribute("favorites", favoriteService.list(SecurityUtils.getUserId()));
        return "portal/favorites";
    }
}