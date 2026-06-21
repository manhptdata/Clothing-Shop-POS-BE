package com.sapo.mock.clothing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Đăng ký PermissionInterceptor vào Spring MVC.
 * Tất cả request đều bị chặn để kiểm tra quyền, trừ whitelist.
 */
@Configuration
public class PermissionInterceptorConfiguration implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    public PermissionInterceptorConfiguration(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] whiteList = {
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**",
            "/storage/**"
        };
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(whiteList);
    }
}
