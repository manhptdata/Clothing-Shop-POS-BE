package com.sapo.mock.clothing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapo.mock.clothing.common.dto.response.RestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi 401 Unauthorized theo format RestResponse chuẩn của hệ thống.
 * Được kích hoạt khi request vào endpoint bảo mật mà không có hoặc sai token.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        System.out.println(">>> 401 Unauthorized at URI: " + request.getRequestURI());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        RestResponse<Object> errorResponse = new RestResponse<>();
        errorResponse.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError("Unauthorized");
        errorResponse.setMessage("Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        errorResponse.setData(null);

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
