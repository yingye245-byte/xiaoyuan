package com.campus.secondhand.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录成功处理：管理员进后台看板，普通用户回首页；若存在被拦截前的目标地址则优先回去
 */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    public LoginSuccessHandler() {
        super("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String target;
        if (savedRequest != null) {
            target = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
        } else {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            target = admin ? "/admin/dashboard" : "/";
        }
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}