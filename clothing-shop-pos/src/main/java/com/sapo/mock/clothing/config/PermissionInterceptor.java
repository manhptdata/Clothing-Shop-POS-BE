package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.domain.entity.Permission;
import com.sapo.mock.clothing.domain.entity.Role;
import com.sapo.mock.clothing.domain.entity.User;
import com.sapo.mock.clothing.service.UserService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.error.PermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Interceptor kiểm tra quyền hạn (Permission) của người dùng trước mỗi request.
 * So khớp (apiPath, method) của request với danh sách permissions trong role của user.
 * Phân quyền thực hiện từ phía API, không chỉ ẩn menu ở frontend.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public PermissionInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();
        String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse("");

        if (currentUserEmail.isEmpty()) {
            return true;
        }

        User currentUser = userService.getUserByEmail(currentUserEmail);
        if (currentUser == null) {
            throw new PermissionException("Không tìm thấy thông tin người dùng");
        }

        Role userRole = currentUser.getRole();
        if (userRole == null) {
            throw new PermissionException("Tài khoản chưa được gán vai trò, vui lòng liên hệ quản trị viên");
        }

        List<Permission> rolePermissions = userRole.getPermissions();
        if (rolePermissions == null || rolePermissions.isEmpty()) {
            throw new PermissionException("Vai trò của bạn chưa có quyền truy cập nào được cấu hình");
        }

        boolean isPermitted = rolePermissions.stream().anyMatch(permission ->
            requestURI.startsWith(permission.getApiPath())
            && httpMethod.equalsIgnoreCase(permission.getMethod())
        );

        if (!isPermitted) {
            throw new PermissionException("Bạn không có quyền thực hiện thao tác này");
        }

        return true;
    }
}
