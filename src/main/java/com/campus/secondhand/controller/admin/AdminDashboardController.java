package com.campus.secondhand.controller.admin;

import com.campus.secondhand.service.ItemService;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.StatService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 后台首页：数据看板（ECharts）
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StatService statService;
    private final ItemService itemService;
    private final OrderService orderService;
    private final MenuService menuService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("overview", statService.overview());
        model.addAttribute("waitAuditItems", itemService.pendingAudit(5));
        model.addAttribute("recentOrders", orderService.pageAdmin(null, null, 1, 5).getRecords());
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "dashboard");
        return "admin/dashboard";
    }

    /** 待审核数量角标接口 */
    @GetMapping("/api/wait-audit-count")
    @ResponseBody
    public long waitAuditCount() {
        return itemService.countByStatus(com.campus.secondhand.common.Constants.ITEM_WAIT_AUDIT);
    }
}