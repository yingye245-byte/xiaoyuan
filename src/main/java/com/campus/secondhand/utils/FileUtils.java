package com.campus.secondhand.utils;

import com.campus.secondhand.config.FileProperties;
import com.campus.secondhand.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件存储工具：统一校验扩展名与大小，返回可直接写库的访问路径
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUtils {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FileProperties fileProperties;

    /**
     * 保存单个文件
     *
     * @param file   上传文件
     * @param subDir 业务子目录，如 item / avatar
     * @return 形如 /upload/item/20240101/xxx.jpg 的访问路径
     */
    public String save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        List<String> allowed = Arrays.asList(fileProperties.getAllowedExt().split(","));
        if (!allowed.contains(ext)) {
            throw new BusinessException("仅支持 " + fileProperties.getAllowedExt() + " 格式的图片");
        }
        long maxBytes = fileProperties.getMaxSize() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("单张图片不能超过 " + fileProperties.getMaxSize() + "MB");
        }
        String relativeDir = subDir + "/" + LocalDate.now().format(DAY_FORMAT);
        Path dir = Paths.get(fileProperties.normalizedPath(), relativeDir);
        try {
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());
            String accessPath = fileProperties.getAccessPrefix() + relativeDir + "/" + fileName;
            log.info("文件已保存：{} -> {}", original, accessPath);
            return accessPath;
        } catch (IOException e) {
            log.error("文件保存失败：{}", original, e);
            throw new BusinessException("图片保存失败，请稍后重试");
        }
    }

    /**
     * 删除本地文件（路径非法或文件不存在时静默返回，不阻断业务）
     */
    public void delete(String accessPath) {
        if (accessPath == null || accessPath.isBlank()) {
            return;
        }
        String prefix = fileProperties.getAccessPrefix();
        if (!accessPath.startsWith(prefix)) {
            return;
        }
        String relative = accessPath.substring(prefix.length());
        File target = Paths.get(fileProperties.normalizedPath(), relative).toFile();
        if (target.exists() && !target.delete()) {
            log.warn("文件删除失败：{}", target.getAbsolutePath());
        }
    }
}