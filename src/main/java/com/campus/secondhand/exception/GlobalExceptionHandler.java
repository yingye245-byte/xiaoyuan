package com.campus.secondhand.exception;

import com.campus.secondhand.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：页面请求返回错误页，AJAX 请求返回统一 JSON
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("业务异常 uri={} code={} msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        if (isAjax(request)) {
            return R.fail(e.getCode(), e.getMessage());
        }
        // 401/403/404 使用真实 HTTP 状态码，便于浏览器与监控识别；其余业务错误按 200 渲染提示页
        if (e.getCode() == 401 || e.getCode() == 403 || e.getCode() == 404) {
            response.setStatus(e.getCode());
        }
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", e.getMessage());
        return mv;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValid(Exception e, HttpServletRequest request) {
        String msg;
        if (e instanceof MethodArgumentNotValidException) {
            msg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage).collect(Collectors.joining("；"));
        } else {
            msg = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage).collect(Collectors.joining("；"));
        }
        if (msg.isEmpty()) {
            msg = "参数校验失败";
        }
        log.info("参数校验失败 uri={} msg={}", request.getRequestURI(), msg);
        if (isAjax(request)) {
            return R.fail(400, msg);
        }
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", msg);
        return mv;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraint(ConstraintViolationException e, HttpServletRequest request) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.joining("；"));
        if (isAjax(request)) {
            return R.fail(400, msg);
        }
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", msg);
        return mv;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUpload(MaxUploadSizeExceededException e, HttpServletRequest request) {
        String msg = "上传文件过大，单张图片不能超过 5MB，总数不超过 25MB";
        if (isAjax(request)) {
            return R.fail(400, msg);
        }
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", msg);
        return mv;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), e);
        if (isAjax(request)) {
            return R.fail(500, "服务器开小差了，请稍后再试");
        }
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("message", "服务器内部错误，请联系管理员");
        return mv;
    }

    /**
     * 判断是否为 AJAX / API 请求，用于决定返回 JSON 还是错误页
     */
    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }
}