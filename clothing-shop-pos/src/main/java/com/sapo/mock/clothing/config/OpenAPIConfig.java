package com.sapo.mock.clothing.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger/OpenAPI với Bearer token authentication.
 * Truy cập: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
@OpenAPIDefinition(info = @Info(
    title = "Clothing Shop POS API",
    version = "1.0.0",
    description = "Hệ thống Point of Sale cho cửa hàng quần áo — SAPO Mock Project",
    contact = @Contact(name = "Team Dev", email = "dev@sapo.vn")
))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Nhập JWT access token nhận được sau khi đăng nhập"
)
public class OpenAPIConfig {
}
