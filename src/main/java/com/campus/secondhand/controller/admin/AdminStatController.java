package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.R;
import com.campus.secondhand.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台看板数据接口：只读 JSON，供 ECharts 异步加载
 */
@RestController
@RequestMapping("/admin/api/stat")
@RequiredArgsConstructor
public class AdminStatController {

    private final StatService statService;

    /** 概览指标 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(statService.overview());
    }

    /** 分类分布（饼图） */
    @GetMapping("/category")
    public R<Object> category() {
        return R.ok(statService.categoryDistribution());
    }

    /** 物品发布趋势（折线图） */
    @GetMapping("/item-trend")
    public R<Object> itemTrend() {
        return R.ok(statService.itemTrend());
    }

    /** 订单趋势（柱状图） */
    @GetMapping("/order-trend")
    public R<Object> orderTrend() {
        return R.ok(statService.orderTrend());
    }

    /** 订单状态分布（环形图） */
    @GetMapping("/order-status")
    public R<Object> orderStatus() {
        return R.ok(statService.orderStatusDistribution());
    }

    /** 成交额 TOP5 卖家（条形图） */
    @GetMapping("/top-sellers")
    public R<Object> topSellers() {
        return R.ok(statService.topSellers());
    }

    /** 近 15 天日统计（折线图） */
    @GetMapping("/daily")
    public R<Object> daily() {
        return R.ok(statService.recentDaily(15));
    }

    /** 一次性返回看板全部数据，减少请求数 */
    @GetMapping("/all")
    public R<Map<String, Object>> all() {
        Map<String, Object> data = new HashMap<>();
        data.put("overview", statService.overview());
        data.put("category", statService.categoryDistribution());
        data.put("itemTrend", statService.itemTrend());
        data.put("orderTrend", statService.orderTrend());
        data.put("orderStatus", statService.orderStatusDistribution());
        data.put("topSellers", statService.topSellers());
        data.put("daily", statService.recentDaily(15));
        return R.ok(data);
    }
}