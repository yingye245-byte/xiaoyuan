package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.MailLog;
import com.campus.secondhand.entity.Menu;
import com.campus.secondhand.entity.Role;
import com.campus.secondhand.mapper.MailLogMapper;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 后台系统管理：角色、菜单权限（只读展示）与邮件日志
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSysController {

    private final MenuService menuService;
    private final MailLogMapper mailLogMapper;

    @GetMapping("/roles")
    public String roles(Model model) {
        List<Role> roles = menuService.listRoles();
        model.addAttribute("roles", roles);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "roles");
        return "admin/roles";
    }

    @GetMapping("/menus")
    public String menus(Model model) {
        List<Menu> all = menuService.listAll();
        model.addAttribute("allMenus", all);
        model.addAttribute("menuTree", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "menus");
        return "admin/menus";
    }

    /**
     * 邮件日志：展示 MQ 异步邮件的发送结果，答辩时用于证明邮件链路真实可用
     */
    @GetMapping("/mail-logs")
    public String mailLogs(@RequestParam(defaultValue = "1") Integer pageNum, Model model) {
        IPage<MailLog> page = mailLogMapper.selectPage(new Page<>(pageNum, 10),
                new LambdaQueryWrapper<MailLog>().orderByDesc(MailLog::getId));
        model.addAttribute("page", page);
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "mailLogs");
        return "admin/mail-logs";
    }
}