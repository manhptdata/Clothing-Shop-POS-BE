package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.user.service.UserService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.exception.PermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor kiểm tra vai trò (role) của người dùng trước mỗi request.
 * Hiện tại chỉ kiểm tra user có active không.
 * Team có thể mở rộng thêm logic phân quyền theo role tại đây.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public PermissionInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        String currentUsername = SecurityUtil.getCurrentUserLogin().orElse("");
        if (currentUsername.isEmpty()) {
            return true; // Chưa đăng nhập → để SecurityFilterChain xử lý
        }

        User currentUser = userService.getUserByUsername(currentUsername);
        if (currentUser == null) {
            throw new PermissionException("Không tìm thấy thông tin người dùng");
        }
        if (!currentUser.isActive()) {
            throw new PermissionException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        // TODO: Team mở rộng logic phân quyền theo role tại đây
        // Ví dụ: kiểm tra currentUser.getRole() có được truy cập requestURI không

        return true;
    }
}
