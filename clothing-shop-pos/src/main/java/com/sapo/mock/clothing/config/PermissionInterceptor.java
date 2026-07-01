package com.sapo.mock.clothing.config;

import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.user.service.UserService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.constant.RoleEnum;
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

        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/notifications") || requestURI.startsWith("/api/shifts")) {
            return true;
        }

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

        RoleEnum role = currentUser.getRole();

        // ROLE_ADMIN: Full access
        if (role == RoleEnum.ROLE_ADMIN) {
            return true;
        }

        // ROLE_SALE: Access to products, categories, orders, customers, recommendations (excluding rebuild)
        if (role == RoleEnum.ROLE_SALE) {
            if (requestURI.startsWith("/api/products") ||
                requestURI.startsWith("/api/categories") ||
                requestURI.startsWith("/api/orders") ||
                requestURI.startsWith("/api/crm/customers") ||
                (requestURI.startsWith("/api/recommendations") && !requestURI.startsWith("/api/recommendations/rebuild"))) {
                return true;
            }
        }

        // ROLE_CS: Access to CRM (customers, campaigns, groups) and orders
        if (role == RoleEnum.ROLE_CS) {
            if (requestURI.startsWith("/api/crm") ||
                requestURI.startsWith("/api/orders")) {
                return true;
            }
        }

        // ROLE_WH: Access to products, categories, stock receipts, suppliers
        if (role == RoleEnum.ROLE_WH) {
            if (requestURI.startsWith("/api/products") ||
                requestURI.startsWith("/api/categories") ||
                requestURI.startsWith("/api/receipts") ||
                requestURI.startsWith("/api/suppliers")) {
                return true;
            }
        }

        throw new PermissionException("Bạn không có quyền truy cập chức năng này.");
    }
}
