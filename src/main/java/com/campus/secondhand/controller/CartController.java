package com.campus.secondhand.controller;

import com.campus.secondhand.service.CartService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 购物车控制器：购物车数据全部存放在 Redis，不落库
 */
@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart")
    public String cart(Model model) {
        Long userId = SecurityUtils.getUserId();
        model.addAttribute("items", cartService.list(userId));
        model.addAttribute("totalAmount", cartService.totalAmount(userId));
        return "portal/cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long itemId,
                      @RequestParam(required = false, defaultValue = "false") boolean fromList,
                      RedirectAttributes redirectAttributes) {
        try {
            cartService.add(SecurityUtils.getUserId(), itemId);
            redirectAttributes.addFlashAttribute("msg", "已加入购物车");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return fromList ? "redirect:/cart" : "redirect:/item/" + itemId;
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long itemId, RedirectAttributes redirectAttributes) {
        cartService.remove(SecurityUtils.getUserId(), itemId);
        redirectAttributes.addFlashAttribute("msg", "已从购物车移除");
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clear(RedirectAttributes redirectAttributes) {
        cartService.clear(SecurityUtils.getUserId());
        redirectAttributes.addFlashAttribute("msg", "购物车已清空");
        return "redirect:/cart";
    }
}