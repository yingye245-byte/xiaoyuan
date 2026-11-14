package com.campus.secondhand.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 状态页映射。
 *
 * <p>Spring Security 的 accessDeniedHandler 会重定向到 /403，
 * 但 /403 本身必须有映射，否则会走到 404 分支（把 403 页面变成 404）。
 * 这里显式渲染 error/403、error/404 模板，并带上正确的 HTTP 状态码。</p>
 */
@Controller
public class StatusPageController {

    @GetMapping("/403")
    public ModelAndView forbidden() {
        ModelAndView mv = new ModelAndView("error/403");
        mv.setStatus(HttpStatus.FORBIDDEN);
        return mv;
    }

    @GetMapping("/404")
    public ModelAndView notFound() {
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        return mv;
    }
}