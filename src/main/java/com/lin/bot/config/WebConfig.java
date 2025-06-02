package com.lin.bot.config;


import com.lin.bot.handler.ApiTokenHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * @Author Lin.
 * @Date 2024/12/28
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final ApiTokenHandler apiTokenHandler;

    public WebConfig(ApiTokenHandler apiTokenHandler) {
        this.apiTokenHandler = apiTokenHandler;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 确保路径格式统一，避免系统路径分隔符问题
        String baseDir = "files" + File.separator + "images" + File.separator;

        // 校正路径格式，确保分隔符统一
        String correctedPath = baseDir.replace("\\", "/");

        // 确保路径结尾有一个斜杠
        if (!correctedPath.endsWith("/")) {
            correctedPath += "/";
        }

        log.info("Corrected image resource path: {}", correctedPath);

        // 将路径映射为静态资源路径，"file:" 前缀表示读取本地文件系统
        registry.addResourceHandler("/img/**")
                .addResourceLocations("file:" + correctedPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiTokenHandler)
                .addPathPatterns("/api/**");
    }
}

