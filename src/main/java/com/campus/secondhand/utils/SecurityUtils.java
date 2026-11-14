package com.campus.secondhand.utils;

import com.campus.secondhand.common.Constants;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类，所有 Controller/Service 取用户信息统一走这里，禁止从请求参数取 userId
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    public static boolean isLogin() {
        return getLoginUser() != null;
    }

    public static Long getUserId() {
        LoginUser user = getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录后再操作");
        }
        return user.getId();
    }

    public static Long getUserIdOrNull() {
        LoginUser user = getLoginUser();
        return user == null ? null : user.getId();
    }

    public static String getNickname() {
        LoginUser user = getLoginUser();
        return user == null ? "游客" : user.getNickname();
    }

    public static boolean isAdmin() {
        LoginUser user = getLoginUser();
        return user != null && user.hasRole(Constants.ROLE_ADMIN);
    }

    public static boolean hasRole(String role) {
        LoginUser user = getLoginUser();
        return user != null && user.hasRole(role);
    }
}