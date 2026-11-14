package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.dto.PasswordDTO;
import com.campus.secondhand.dto.ProfileDTO;
import com.campus.secondhand.entity.Message;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.HistoryService;
import com.campus.secondhand.service.MessageService;
import com.campus.secondhand.service.UserService;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * 个人中心控制器：资料、密码、头像、站内消息、浏览记录
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MessageService messageService;
    private final HistoryService historyService;

    @GetMapping("/user/profile")
    public String profile(Model model) {
        User user = userService.getById(SecurityUtils.getUserId());
        ProfileDTO dto = new ProfileDTO();
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setCampus(user.getCampus());
        dto.setStudentNo(user.getStudentNo());
        model.addAttribute("profileDTO", dto);
        model.addAttribute("user", user);
        return "portal/profile";
    }

    @PostMapping("/user/profile")
    public String updateProfile(@Valid @ModelAttribute("profileDTO") ProfileDTO dto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("user", userService.getById(SecurityUtils.getUserId()));
            return "portal/profile";
        }
        try {
            userService.updateProfile(SecurityUtils.getUserId(), dto);
            redirectAttributes.addFlashAttribute("msg", "资料已更新");
            return "redirect:/user/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", userService.getById(SecurityUtils.getUserId()));
            return "portal/profile";
        }
    }

    @GetMapping("/user/password")
    public String passwordPage(Model model) {
        model.addAttribute("passwordDTO", new PasswordDTO());
        return "portal/password";
    }

    @PostMapping("/user/password")
    public String changePassword(@Valid @ModelAttribute("passwordDTO") PasswordDTO dto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "portal/password";
        }
        try {
            userService.changePassword(SecurityUtils.getUserId(), dto);
            redirectAttributes.addFlashAttribute("msg", "密码修改成功，请牢记新密码");
            return "redirect:/user/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "portal/password";
        }
    }

    @PostMapping("/user/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.updateAvatar(SecurityUtils.getUserId(), file);
            redirectAttributes.addFlashAttribute("msg", "头像已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/profile";
    }

    @GetMapping("/user/messages")
    public String messages(@RequestParam(defaultValue = "1") Integer pageNum, Model model) {
        Long userId = SecurityUtils.getUserId();
        IPage<Message> page = messageService.page(userId, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("unread", messageService.unreadCount(userId));
        return "portal/messages";
    }

    @PostMapping("/user/messages/{id}/read")
    public String readMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        messageService.markRead(id, SecurityUtils.getUserId());
        return "redirect:/user/messages";
    }

    /** 点击消息：标记已读后跳到消息关联的订单 / 物品页面 */
    @PostMapping("/user/messages/{id}/open")
    public String openMessage(@PathVariable Long id) {
        String target = messageService.markReadAndGetTarget(id, SecurityUtils.getUserId());
        return "redirect:" + target;
    }

    @PostMapping("/user/messages/read-all")
    public String readAll(RedirectAttributes redirectAttributes) {
        messageService.markAllRead(SecurityUtils.getUserId());
        redirectAttributes.addFlashAttribute("msg", "全部消息已标记为已读");
        return "redirect:/user/messages";
    }

    @GetMapping("/user/history")
    public String history(Model model) {
        model.addAttribute("items", historyService.recent(SecurityUtils.getUserId(), 20));
        return "portal/history";
    }

    /** 页头未读消息数（供页面轮询/角标使用） */
    @GetMapping("/user/messages/unread")
    @ResponseBody
    public long unread() {
        Long userId = SecurityUtils.getUserIdOrNull();
        return userId == null ? 0 : messageService.unreadCount(userId);
    }
}