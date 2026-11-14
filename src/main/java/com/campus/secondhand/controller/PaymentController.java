package com.campus.secondhand.controller;

import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.entity.Payment;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.PaymentService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;

/**
 * 收银台：在线支付入口（担保交易）
 */
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @Value("${app.order-timeout-minutes:30}")
    private int orderTimeoutMinutes;

    /**
     * 收银台页面：展示订单、金额、支付渠道与担保说明
     */
    @GetMapping("/pay/{orderId}")
    public String cashier(@PathVariable Long orderId, Model model) {
        Long userId = SecurityUtils.getUserId();
        Payment payment = paymentService.prepare(orderId, userId);
        Orders order = orderService.detail(orderId, userId);
        long deadlineMillis = order.getCreateTime()
                .plusMinutes(orderTimeoutMinutes)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("channels", Constants.PAY_CHANNELS);
        model.addAttribute("deadlineMillis", deadlineMillis);
        model.addAttribute("timeoutMinutes", orderTimeoutMinutes);
        return "portal/pay";
    }

    /**
     * 确认支付（模拟网关 → 验签 → 资金进入平台托管）
     */
    @PostMapping("/pay/{orderId}/confirm")
    public String confirm(@PathVariable Long orderId,
                          @RequestParam("payNo") String payNo,
                          @RequestParam("channel") String channel,
                          RedirectAttributes redirectAttributes) {
        Long userId = SecurityUtils.getUserId();
        paymentService.confirm(payNo, userId, channel);
        redirectAttributes.addFlashAttribute("success",
                "支付成功，¥" + orderService.detail(orderId, userId).getAmount()
                        + " 已由平台托管，确认收货后结算给卖家");
        return "redirect:/order/" + orderId;
    }
}