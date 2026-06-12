package com.sapo.mock.clothing.util;

import com.sapo.mock.clothing.domain.response.RestResponse;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Tự động bọc mọi response thành format RestResponse chuẩn của hệ thống.
 * Lấy message từ annotation @ApiMessage trên method controller nếu có.
 */
@RestControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        HttpServletResponse servletResponse =
            ((ServletServerHttpResponse) response).getServletResponse();
        int statusCode = servletResponse.getStatus();

        String path = request.getURI().getPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return body;
        }

        // Không bọc nếu body đã là RestResponse (tránh double wrap) hoặc String
        if (body instanceof RestResponse || body instanceof String) {
            return body;
        }

        RestResponse<Object> wrappedResponse = new RestResponse<>();
        wrappedResponse.setStatusCode(statusCode);
        wrappedResponse.setData(body);

        // Lấy message từ @ApiMessage annotation trên method
        ApiMessage apiMessage = returnType.getMethodAnnotation(ApiMessage.class);
        wrappedResponse.setMessage(apiMessage != null ? apiMessage.value() : "Call API successfully");

        return wrappedResponse;
    }
}
