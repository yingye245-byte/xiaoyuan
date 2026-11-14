package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.utils.StatusUtils;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.Orders;
import com.campus.secondhand.entity.Payment;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrdersMapper;
import com.campus.secondhand.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 在线支付 + 担保交易（资金托管）
 * <p>
 * 演示说明：本机没有真实商户号，因此用一个「模拟网关」实现完整的在线支付链路，
 * 但订单与资金的语义与真实平台一致：
 * <pre>
 * 下单 → 收银台选择渠道 → 网关回调（验签）→ 支付成功，款项进入平台托管
 *      → 买家确认收货 → 订单完成，平台把托管款结算给卖家
 * 超时未支付 / 买家取消 → 订单关闭，托管前不会产生扣款
 * </pre>
 * 关键保障：
 * 1. 金额只以服务端订单金额为准，前端传什么都无效；
 * 2. 回调带 HMAC-SHA256 签名，验签失败一律拒绝；
 * 3. 支付流水与订单状态各自 CAS 更新，重复回调幂等，不会重复入账；
 * 4. 支付成功才把物品置为已售出，失败或超时由定时任务回滚物品状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter PAY_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentMapper paymentMapper;
    private final OrdersMapper ordersMapper;
    private final ItemMapper itemMapper;
    private final OrderService orderService;

    /** 模拟网关的签名密钥，生产环境必须用环境变量覆盖 */
    @Value("${app.payment.mock-secret:campus-secondhand-mock-pay-secret}")
    private String mockSecret;

    /**
     * 打开收银台：为待支付订单准备一条支付流水（同一订单重复进入收银台会复用未支付的流水）
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment prepare(Long orderId, Long userId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(403, "只能支付自己的订单");
        }
        if (!StatusUtils.is(order.getStatus(), Constants.ORDER_WAIT_PAY)) {
            throw new BusinessException("该订单当前状态不需要支付");
        }
        Payment existed = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, Constants.PAY_WAIT)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (existed != null) {
            if (existed.getAmount() == null || existed.getAmount().compareTo(order.getAmount()) != 0) {
                existed.setAmount(order.getAmount());
                paymentMapper.updateById(existed);
            }
            return existed;
        }
        // 订单实体的 itemTitle 是连表查询才填充的快照字段，单表 selectById 拿不到，这里按 item_id 回查一次
        Item item = itemMapper.selectById(order.getItemId());
        Payment payment = new Payment();
        payment.setPayNo(generatePayNo());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setItemId(order.getItemId());
        payment.setItemTitle(item != null ? item.getTitle() : "物品已删除");
        payment.setUserId(order.getBuyerId());
        payment.setAmount(order.getAmount());
        payment.setChannel(Constants.PAY_CHANNEL_ALIPAY);
        payment.setStatus(Constants.PAY_WAIT);
        paymentMapper.insert(payment);
        return payment;
    }

    public Payment getOwnPayment(String payNo, Long userId) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPayNo, payNo));
        if (payment == null) {
            throw new BusinessException(404, "支付流水不存在");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该支付流水");
        }
        return payment;
    }

    /**
     * 模拟网关支付：校验渠道 → 生成交易号与签名 → 验签 → 更新流水 → 订单转为已支付（款项托管）
     *
     * @return 支付成功后的订单 id，便于页面跳回订单详情
     */
    @Transactional(rollbackFor = Exception.class)
    public Long confirm(String payNo, Long userId, String channel) {
        Payment payment = getOwnPayment(payNo, userId);
        if (StatusUtils.is(payment.getStatus(), Constants.PAY_SUCCESS)) {
            // 幂等：重复提交/重复回调直接返回成功，不重复入账
            return payment.getOrderId();
        }
        if (!StatusUtils.is(payment.getStatus(), Constants.PAY_WAIT)) {
            throw new BusinessException("该支付流水已关闭，请重新下单");
        }
        if (channel == null || channel.isBlank()) {
            throw new BusinessException("请选择支付方式");
        }
        boolean knownChannel = false;
        for (String[] item : Constants.PAY_CHANNELS) {
            if (item[0].equals(channel)) {
                knownChannel = true;
                break;
            }
        }
        if (!knownChannel) {
            throw new BusinessException("不支持的支付方式");
        }

        Orders order = ordersMapper.selectById(payment.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (payment.getAmount().compareTo(order.getAmount()) != 0) {
            throw new BusinessException("支付金额与订单金额不一致，已拦截");
        }

        // 模拟第三方网关：生成交易号并用密钥签名
        String tradeNo = channel + LocalDateTime.now().format(PAY_NO_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 9999);
        String sign = sign(payment.getPayNo(), payment.getOrderNo(), payment.getAmount(), tradeNo);
        if (!verify(sign, payment.getPayNo(), payment.getOrderNo(), payment.getAmount(), tradeNo)) {
            throw new BusinessException("支付回调验签失败，已拒绝本次支付");
        }

        int rows = paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getStatus, Constants.PAY_WAIT)
                .set(Payment::getStatus, Constants.PAY_SUCCESS)
                .set(Payment::getChannel, channel)
                .set(Payment::getTradeNo, tradeNo)
                .set(Payment::getNotifyTime, LocalDateTime.now()));
        if (rows == 0) {
            // 并发重复回调：以先到的那次为准，仍按成功处理
            log.info("支付流水 {} 已被其他线程处理，按幂等返回", payNo);
            return payment.getOrderId();
        }

        orderService.markPaid(payment.getOrderId());
        log.info("订单 {} 在线支付成功：payNo={} channel={} amount={} tradeNo={}",
                order.getOrderNo(), payNo, channel, payment.getAmount(), tradeNo);
        return payment.getOrderId();
    }

    /**
     * 后台支付流水分页
     */
    public IPage<Payment> pageAdmin(String keyword, Integer status, String channel, int pageNum, int pageSize) {
        IPage<Payment> page = new Page<>(Math.max(pageNum, 1), pageSize);
        return paymentMapper.selectPaymentPage(page,
                keyword == null ? null : keyword.trim(), status, channel);
    }

    /**
     * 托管中的资金总额
     */
    public BigDecimal escrowAmount() {
        BigDecimal amount = paymentMapper.selectEscrowAmount();
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 订单取消/超时关闭时，把该订单下未支付的流水一并关闭，避免二次支付
     */
    public int closePendingByOrder(Long orderId) {
        return paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, Constants.PAY_WAIT)
                .set(Payment::getStatus, Constants.PAY_CLOSED));
    }

    private String generatePayNo() {
        return "PAY" + LocalDateTime.now().format(PAY_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private String sign(String payNo, String orderNo, BigDecimal amount, String tradeNo) {
        String content = payNo + "|" + orderNo + "|" + amount.toPlainString() + "|" + tradeNo;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(mockSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("支付签名计算失败", e);
            throw new BusinessException("支付签名异常，请稍后重试");
        }
    }

    private boolean verify(String sign, String payNo, String orderNo, BigDecimal amount, String tradeNo) {
        String expected = sign(payNo, orderNo, amount, tradeNo);
        return expected.equalsIgnoreCase(sign);
    }

    /**
     * 供订单详情页展示：该订单是否已完成在线支付
     */
    public Payment getSuccessPayment(Long orderId) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, Constants.PAY_SUCCESS)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
    }

    /**
     * 收银台需要的物品信息
     */
    public Item getItem(Long itemId) {
        return itemMapper.selectById(itemId);
    }
}