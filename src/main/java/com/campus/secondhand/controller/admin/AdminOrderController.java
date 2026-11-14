package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.service.OrderService;
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
 * 后台订单管理：全量订单查询、超时订单强制关闭
 */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final MenuService menuService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       Model model) {
        IPage<Orders> page = orderService.pageAdmin(status, keyword, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "orders");
        return "admin/orders";
    }

    /**
     * 强制关闭待支付订单（异常订单处理）
     */
    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean closed = orderService.autoCancel(id);
        redirectAttributes.addFlashAttribute(closed ? "msg" : "error",
                closed ? "订单已关闭，物品重新上架" : "该订单无需关闭");
        return "redirect:/admin/orders";
    }
}