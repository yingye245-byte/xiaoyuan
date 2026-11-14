package com.campus.secondhand.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置，对应 application.yml 中的 file.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /** 物理存储根目录，必须以分隔符结尾 */
    private String uploadPath = "D:/campus-upload/";

    /** 允许的扩展名，逗号分隔 */
    private String allowedExt = "jpg,jpeg,png,gif,webp";

    /** 单文件大小上限（字节） */
    private Long maxSize = 5242880L;

    /** 访问前缀，与 WebMvcConfig 中的资源映射保持一致 */
    private String accessPrefix = "/upload/";

    public String normalizedPath() {
        String path = this.uploadPath == null ? "D:/campus-upload/" : this.uploadPath.trim();
        if (!path.endsWith("/") && !path.endsWith("\\")) {
            path = path + "/";
        }
        return path.replace("\\", "/");
    }
}