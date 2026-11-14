package com.campus.secondhand.common;

import com.campus.secondhand.service.MessageService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局模型增强：所有页面都能拿到「未读消息数」，避免每个 Controller 重复塞值。
 *
 * <p>注意：不要在这里往模型里放 Map 再在模板里用 {@code ${map.key}} 取值——
 * Thymeleaf 的 SpEL 上下文没有注册 MapAccessor，对 Map 做属性访问会抛
 * EL1008E（Property or field 'xxx' cannot be found on object of type 'java.util.HashMap'）。
 * 需要回显筛选条件时，直接使用 Controller 里绑定的 DTO（例如 {@code ${query.keyword}}）。</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final MessageService messageService;

    @ModelAttribute("unreadCount")
    public long unreadCount() {
        Long userId = SecurityUtils.getUserIdOrNull();
        return userId == null ? 0 : messageService.unreadCount(userId);
    }
}