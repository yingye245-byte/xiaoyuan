package com.campus.secondhand.service;

import com.campus.secondhand.common.Constants;
import com.campus.secondhand.entity.MailLog;
import com.campus.secondhand.mapper.MailLogMapper;
import com.campus.secondhand.mq.MailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * 邮件服务：Thymeleaf 渲染 HTML 模板 + JavaMailSender 发送 + 邮件日志留痕
 *
 * <p>app.mail.mock=true 时只渲染模板并写日志不真正外发，
 * 保证机房断网、未配置邮箱授权码时项目仍可完整演示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailLogMapper mailLogMapper;

    @Value("${app.mail.mock:true}")
    private boolean mock;

    @Value("${spring.mail.username:no-reply@campus.local}")
    private String from;

    /**
     * 渲染并发送邮件，失败只记日志不抛异常（消息消费不应无限重试）
     */
    public void send(MailMessage message) {
        if (message == null || message.getTo() == null) {
            log.warn("邮件消息缺少收件人，已跳过");
            return;
        }
        String html = render(message);
        MailLog mailLog = new MailLog();
        mailLog.setToEmail(message.getTo());
        mailLog.setSubject(message.getSubject());
        mailLog.setBizType(resolveBizType(message.getTemplate()));
        try {
            if (mock) {
                log.info("[邮件MOCK] to={} subject={} 内容长度={}",
                        message.getTo(), message.getSubject(), html.length());
                mailLog.setStatus(Constants.MAIL_SUCCESS);
            } else {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(message.getTo());
                helper.setSubject(message.getSubject());
                helper.setText(html, true);
                mailSender.send(mimeMessage);
                log.info("邮件发送成功：to={} subject={}", message.getTo(), message.getSubject());
                mailLog.setStatus(Constants.MAIL_SUCCESS);
            }
        } catch (Exception e) {
            log.error("邮件发送失败：to={} subject={}", message.getTo(), message.getSubject(), e);
            mailLog.setStatus(Constants.MAIL_FAIL);
            mailLog.setErrorMsg(truncate(e.getMessage(), 480));
        }
        mailLogMapper.insert(mailLog);
    }

    private String render(MailMessage message) {
        Context context = new Context();
        Map<String, Object> params = message.getParams();
        if (params != null) {
            params.forEach(context::setVariable);
        }
        context.setVariable("nickname", message.getNickname());
        context.setVariable("subject", message.getSubject());
        try {
            return templateEngine.process("mail/" + message.getTemplate(), context);
        } catch (Exception e) {
            log.error("邮件模板渲染失败：{}", message.getTemplate(), e);
            return "<p>" + message.getSubject() + "：模板渲染失败，请查看服务端日志。</p>";
        }
    }

    private String resolveBizType(String template) {
        if (template == null) {
            return "SYSTEM";
        }
        if (template.startsWith("order")) {
            return "ORDER";
        }
        return switch (template) {
            case "welcome" -> "SYSTEM";
            case "audit-result" -> "AUDIT";
            default -> "COMMENT";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}