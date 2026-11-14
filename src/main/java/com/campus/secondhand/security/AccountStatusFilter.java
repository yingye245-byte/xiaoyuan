package com.campus.secondhand.security;

import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.UserMapper;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 账号状态闸门：管理员在后台禁用某个账号后，该账号已登录的会话必须立刻失效。
 *
 * <p>放在 Security 过滤器链之后执行（作为普通 Bean 注册，顺序最靠后），此时
 * SecurityContext 里已有登录信息，直接按主键查一次库即可拿到最新 status；
 * 用户量小的校园项目用一次主键查询换取「禁用即时生效」是划算的。</p>
 */
@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser loginUser) {
            User current = userMapper.selectById(loginUser.getId());
            boolean disabled = current == null || current.getStatus() == null || current.getStatus() != 1;
            if (disabled) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?disabled");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 静态资源与错误页不查库，避免一个页面几十个请求把库打满
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/img/")
                || uri.startsWith("/lib/") || uri.startsWith("/upload/") || uri.startsWith("/error")
                || uri.equals("/favicon.ico") || uri.startsWith("/login") || uri.startsWith("/register");
    }
}