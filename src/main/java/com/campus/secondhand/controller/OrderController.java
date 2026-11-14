package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.entity.Payment;
import com.campus.secondhand.service.ItemService;
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
import java.util.UUID;

/**
 * 订单控制器：下单确认页、创建订单、支付、确认收货、取消
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ItemService itemService;
    private final PaymentService paymentService;

    /** 待支付超时分钟数，与 MQ 延迟队列、定时任务使用同一个配置项 */
    @Value("${app.order-timeout-minutes:30}")
    private int orderTimeoutMinutes;

    /**
     * 下单确认页：生成一次性幂等令牌
     */
    @GetMapping("/order/create")
    public String createPage(@RequestParam Long itemId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Item item = itemService.detail(itemId, SecurityUtils.getUserId());
            model.addAttribute("item", item);
            model.addAttribute("token", UUID.randomUUID().toString().replace("-", ""));
            return "portal/order-create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/items";
        }
    }

    /**
     * 创建订单：token 由确认页下发，Redis SETNX 保证重复点击只生成一笔订单
     */
    @PostMapping("/order/create")
    public String create(@RequestParam Long itemId,
                         @RequestParam String token,
                         RedirectAttributes redirectAttributes) {
        try {
            Long orderId = orderService.create(itemId, SecurityUtils.getUserId(), token);
            redirectAttributes.addFlashAttribute("msg", "下单成功，请在 30 分钟内完成支付");
            return "redirect:/order/" + orderId;
        } catch (Exception e) {
            log.warn("创建订单失败：{}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/item/" + itemId;
        }
    }

    @GetMapping("/order/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Orders order = orderService.detail(id, SecurityUtils.getUserId());
        model.addAttribute("order", order);
        model.addAttribute("isBuyer", order.getBuyerId().equals(SecurityUtils.getUserId()));
        // 待支付倒计时用绝对时间戳交给前端，避免字符串解析在不同浏览器上出现 NaN
        long deadlineMillis = order.getCreateTime()
                .plusMinutes(orderTimeoutMinutes)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        model.addAttribute("deadlineMillis", deadlineMillis);
        model.addAttribute("timeoutMinutes", orderTimeoutMinutes);
        // 已支付订单展示资金托管信息（担保交易）
        Payment payment = paymentService.getSuccessPayment(order.getId());
        model.addAttribute("payment", payment);
        return "portal/order-detail";
    }

    @GetMapping("/orders/mine")
    public String mine(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(required = false) Integer status,
                       Model model) {
        IPage<Orders> page = orderService.pageBuyer(SecurityUtils.getUserId(), status, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        model.addAttribute("view", "buy");
        return "portal/orders";
    }

    @GetMapping("/orders/sold")
    public String sold(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(required = false) Integer status,
                       Model model) {
        IPage<Orders> page = orderService.pageSeller(SecurityUtils.getUserId(), status, pageNum, 10);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        model.addAttribute("view", "sell");
        return "portal/orders";
    }

    /**
     * 兼容旧入口：待支付订单统一跳到在线收银台，不再直接标记已支付
     */
    @PostMapping("/order/{id}/pay")
    public String pay(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Orders order = orderService.detail(id, SecurityUtils.getUserId());
        if (!order.getBuyerId().equals(SecurityUtils.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "只有买家可以支付该订单");
            return "redirect:/order/" + id;
        }
        return "redirect:/pay/" + id;
    }

    @PostMapping("/order/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirm(id, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", "已确认收货，交易完成");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/order/" + id;
    }

    @PostMapping("/order/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id, SecurityUtils.getUserId(), reason);
            redirectAttributes.addFlashAttribute("msg", "订单已取消");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/order/" + id;
    }
}