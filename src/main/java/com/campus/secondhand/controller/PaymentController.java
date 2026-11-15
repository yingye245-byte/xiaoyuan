package com.campus.secondhand.controller;

import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.entity.Payment;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.PaymentService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public String cashier(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        Long userId = SecurityUtils.getUserId();
        Payment payment;
        Orders order;
        try {
            // prepare 内部已校验归属、订单状态与支付时限
            payment = paymentService.prepare(orderId, userId);
            order = orderService.detail(orderId, userId);
        } catch (BusinessException e) {
            // 越权（403）/不存在（404）直接交给全局异常处理器返回真实状态码，
            // 其余（已支付、已取消、已超时）回订单详情并提示原因，不必停在 500 错误页
            if (e.getCode() == 403 || e.getCode() == 404) {
                throw e;
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/order/" + orderId;
        }
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
        // 支付流水必须属于路径上的订单，避免「用自己的 A 单流水给 B 单付款」的错位提示
        Payment payment = paymentService.getOwnPayment(payNo, userId);
        if (!payment.getOrderId().equals(orderId)) {
            redirectAttributes.addFlashAttribute("error", "支付流水与订单不匹配，请重新打开收银台");
            return "redirect:/order/" + orderId;
        }
        try {
            Long paidOrderId = paymentService.confirm(payNo, userId, channel);
            // 金额与跳转都以服务端返回的订单为准，不信任路径参数
            Orders paidOrder = orderService.detail(paidOrderId, userId);
            redirectAttributes.addFlashAttribute("msg",
                    "支付成功，¥" + paidOrder.getAmount() + " 已由平台托管，确认收货后结算给卖家");
            return "redirect:/order/" + paidOrderId;
        } catch (BusinessException e) {
            log.warn("用户 {} 支付失败：payNo={} 原因={}", userId, payNo, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/order/" + orderId;
        }
    }
}