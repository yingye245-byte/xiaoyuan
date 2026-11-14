package com.campus.secondhand.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.Payment;
import com.campus.secondhand.service.MenuService;
import com.campus.secondhand.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 后台支付流水（对账）
 */
@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final MenuService menuService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String channel,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       Model model) {
        IPage<Payment> page = paymentService.pageAdmin(keyword, status, channel, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("channel", channel);
        model.addAttribute("channels", Constants.PAY_CHANNELS);
        model.addAttribute("escrowAmount", paymentService.escrowAmount());
        model.addAttribute("menus", menuService.menusOfUser(com.campus.secondhand.utils.SecurityUtils.getUserId()));
        model.addAttribute("active", "payments");
        return "admin/payments";
    }
}