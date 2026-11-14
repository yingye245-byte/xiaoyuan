package com.campus.secondhand.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置：把上传目录映射为可访问的静态资源
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;

    public WebMvcConfig(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + fileProperties.normalizedPath();
        registry.addResourceHandler(fileProperties.getAccessPrefix() + "**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}