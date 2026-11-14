package com.campus.secondhand.task;

import com.campus.secondhand.service.StatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每日统计任务：每天 00:05 汇总前一天的数据，并清零当天计数器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatTask {

    private final StatService statService;

    @Scheduled(cron = "0 5 0 * * ?")
    public void dailySnapshot() {
        log.info("每日统计任务开始：{}", LocalDateTime.now());
        var stat = statService.recordDailyStat();
        log.info("每日统计任务结束：日期={} 订单={} 成交额={}",
                stat.getStatDate(), stat.getOrderCount(), stat.getTradeAmount());
    }
}