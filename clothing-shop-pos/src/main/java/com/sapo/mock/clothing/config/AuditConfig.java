package com.sapo.mock.clothing.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.sapo.mock.clothing.util.SecurityUtil;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

	@Bean
	public AuditorAware<Integer> auditorProvider() {
		return () -> {

			Integer currentUserId = SecurityUtil.getCurrentUserId();
			return Optional.ofNullable(currentUserId);
		};
	}
}