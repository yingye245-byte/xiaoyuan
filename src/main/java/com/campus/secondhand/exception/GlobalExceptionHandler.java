package com.campus.secondhand.exception;

import com.campus.secondhand.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
        // 业务异常统一使用真实 HTTP 状态码（4xx/5xx），避免越权/重复提交等错误
        // 在浏览器与监控里看起来像「成功返回 200」，只把 2xx 当正常。
        if (e.getCode() >= 400 && e.getCode() <= 599) {
            response.setStatus(e.getCode());
        }
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", e.getMessage());
        return mv;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValid(Exception e, HttpServletRequest request, HttpServletResponse response) {
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
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", msg);
        return mv;
    }

    /**
     * 路径变量 / 查询参数类型不匹配，例如 /order/create?itemId=abc。
     * 默认会落到兜底 Exception 分支返回 500，这里改为 400，提示更准确。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request, HttpServletResponse response) {
        String msg = "参数「" + e.getName() + "」格式不正确";
        log.info("参数类型不匹配 uri={} param={} value={}", request.getRequestURI(), e.getName(), e.getValue());
        if (isAjax(request)) {
            return R.fail(400, msg);
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", msg);
        return mv;
    }

    /**
     * 缺少必填请求参数，例如删除留言时没带 itemId。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request, HttpServletResponse response) {
        String msg = "缺少必填参数「" + e.getParameterName() + "」";
        log.info("缺少参数 uri={} param={}", request.getRequestURI(), e.getParameterName());
        if (isAjax(request)) {
            return R.fail(400, msg);
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        ModelAndView mv = new ModelAndView("error/biz");
        mv.addObject("message", msg);
        return mv;
    }

    /**
     * 请求方法不被支持（如对只接受 POST 的地址发了 GET），返回 405 页面。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        log.info("请求方法不支持 uri={} method={}", request.getRequestURI(), e.getMethod());
        if (isAjax(request)) {
            return R.fail(405, "请求方式不支持");
        }
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        return new ModelAndView("error/405");
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