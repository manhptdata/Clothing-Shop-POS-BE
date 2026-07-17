package com.sapo.mock.clothing.setting.controller;

import com.sapo.mock.clothing.entity.SystemSetting;
import com.sapo.mock.clothing.setting.service.SystemSettingService;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @ApiMessage("Lấy danh sách cấu hình hệ thống thành công")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SystemSetting>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    @PutMapping("/{key}")
    @ApiMessage("Cập nhật cấu hình hệ thống thành công")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<SystemSetting> updateSetting(
            @PathVariable String key,
            @RequestParam String value) {
        return ResponseEntity.ok(systemSettingService.updateSetting(key, value));
    }
}
