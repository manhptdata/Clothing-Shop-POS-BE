package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.util.SecurityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Kích hoạt JPA Auditing.
 * Tự động điền createdBy / updatedBy bằng email của user đang đăng nhập.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> SecurityUtil.getCurrentUserLogin();
    }
}
