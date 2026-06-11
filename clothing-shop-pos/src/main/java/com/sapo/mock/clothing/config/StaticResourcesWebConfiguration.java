package com.sapo.mock.clothing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve các file upload qua HTTP endpoint /storage/**.
 * Ví dụ: file tại uploads/products/img.jpg → GET /storage/products/img.jpg
 */
@Configuration
public class StaticResourcesWebConfiguration implements WebMvcConfigurer {

    @Value("${hoangmelinh.upload-file.base-uri}")
    private String fileBaseUri;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(fileBaseUri);
    }
}
