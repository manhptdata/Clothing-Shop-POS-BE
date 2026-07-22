package com.sapo.mock.clothing.user.controller;

import com.sapo.mock.clothing.user.service.UserService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import com.sapo.mock.clothing.user.dto.request.ChangePasswordRequest;
import com.sapo.mock.clothing.user.dto.request.UpdateProfileRequest;
import com.sapo.mock.clothing.entity.User;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/me/security-pin")
    @ApiMessage("Tạo mã PIN bảo mật ngẫu nhiên thành công")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Map<String, String>> generateSecurityPin() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
        
        String plainPin = userService.generateSecurityPin(username);
        
        Map<String, String> response = new HashMap<>();
        response.put("pin", plainPin);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/security-pin")
    @ApiMessage("Lấy trạng thái mã PIN bảo mật thành công")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getSecurityPin() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
        
        boolean hasPin = userService.hasSecurityPin(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasPin", hasPin);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/security-pin/change")
    @ApiMessage("Đổi mã PIN bảo mật thành công")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Void> changeSecurityPin(@RequestBody Map<String, String> request) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
        
        String newPin = request.get("pin");
        userService.changeSecurityPin(username, newPin);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/password")
    @ApiMessage("Đổi mật khẩu thành công")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
        
        userService.changePassword(username, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/profile")
    @ApiMessage("Cập nhật thông tin nhân viên thành công")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<User> updateUserProfileByAdmin(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        User updatedUser = userService.updateUserProfileByAdmin(id, request);
        return ResponseEntity.ok(updatedUser);
    }
}
