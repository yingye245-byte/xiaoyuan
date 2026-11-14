package com.campus.secondhand.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletResponse;

/**
 * Spring Security 配置：表单登录 + 三级角色鉴权 + 方法级鉴权
 *
 * <p>角色：ROLE_USER 普通用户 / ROLE_SELLER 认证卖家 / ROLE_ADMIN 管理员</p>
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    /**
     * remember-me 的签名密钥：默认值仅供本地开发，生产用 REMEMBER_ME_KEY 环境变量覆盖。
     * 注意它必须固定，否则服务重启后所有「记住我」令牌全部失效。
     */
    @org.springframework.beans.factory.annotation.Value("${app.security.remember-me-key:campus-secondhand-remember-key}")
    private String rememberMeKey;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    /** BCrypt 强哈希，cost=10，与建表脚本中的种子密码一致 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                // 静态资源与公开页面
                .antMatchers("/", "/index", "/login", "/register", "/403", "/404",
                        "/css/**", "/js/**", "/img/**", "/lib/**", "/upload/**",
                        "/favicon.ico", "/error/**").permitAll()
                // 物品浏览与详情：游客可看，扩大曝光
                .antMatchers("/items", "/item/**").permitAll()
                // 后台：仅管理员
                .antMatchers("/admin/**").hasRole("ADMIN")
                // 发布物品：登录用户均可，发布成功后自动升级为认证卖家
                .antMatchers("/items/publish", "/my-items").authenticated()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
                .and()
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
                .and()
                .exceptionHandling()
                .accessDeniedHandler((request, response, ex) -> {
                    if (isAjax(request)) {
                        writeJson(response, 403, "没有访问权限");
                    } else {
                        response.sendRedirect("/403");
                    }
                })
                .authenticationEntryPoint((request, response, ex) -> {
                    if (isAjax(request)) {
                        writeJson(response, 401, "登录状态已失效，请重新登录");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .and()
                .rememberMe()
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(7 * 24 * 3600)
                .key(rememberMeKey)
                .and()
                .headers()
                .frameOptions().sameOrigin();

        return http.build();
    }

    private boolean isAjax(javax.servlet.http.HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void writeJson(HttpServletResponse response, int code, String msg) throws java.io.IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}