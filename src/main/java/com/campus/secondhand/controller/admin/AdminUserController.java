package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Role;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.service.UserService;
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

import java.util.List;

/**
 * 后台用户管理：列表、启用禁用、角色分配
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final MenuService menuService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long roleId,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       Model model) {
        IPage<User> page = userService.pageUsers(keyword, roleId, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("roles", userService.listRoles());
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "users");
        return "admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleEnabled(id);
            redirectAttributes.addFlashAttribute("msg", "账号状态已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/roles")
    public String roles(@PathVariable Long id, Model model) {
        User user = userService.getById(id);
        List<Role> roles = userService.listRoles();
        model.addAttribute("targetUser", user);
        model.addAttribute("roles", roles);
        model.addAttribute("selectedRoleIds", userService.listRoleIds(id));
        model.addAttribute("menus", menuService.menusOfUser(SecurityUtils.getUserId()));
        model.addAttribute("active", "users");
        return "admin/user-roles";
    }

    @PostMapping("/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(required = false) List<Long> roleIds,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.updateRoles(id, roleIds);
            redirectAttributes.addFlashAttribute("msg", "角色分配成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}