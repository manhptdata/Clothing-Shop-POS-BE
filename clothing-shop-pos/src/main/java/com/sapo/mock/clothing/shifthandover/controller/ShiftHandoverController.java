package com.sapo.mock.clothing.shifthandover.controller;

import com.sapo.mock.clothing.entity.ShiftHandover;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.shifthandover.service.ShiftHandoverService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final ShiftHandoverService shiftHandoverService;

    // --- DANH MỤC CA LÀM VIỆC DÀNH CHO ADMIN & POS ---
    @GetMapping("/configs")
    @ApiMessage("Lấy danh mục ca làm việc thành công")
    public ResponseEntity<List<com.sapo.mock.clothing.entity.ShiftConfig>> getActiveShiftConfigs() {
        return ResponseEntity.ok(shiftHandoverService.getActiveShiftConfigs());
    }

    @PostMapping("/configs")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ApiMessage("Thêm ca làm việc mới thành công")
    public ResponseEntity<com.sapo.mock.clothing.entity.ShiftConfig> createShiftConfig(@RequestBody com.sapo.mock.clothing.entity.ShiftConfig config) {
        return ResponseEntity.ok(shiftHandoverService.createShiftConfig(config));
    }

    @PutMapping("/configs/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ApiMessage("Cập nhật ca làm việc thành công")
    public ResponseEntity<com.sapo.mock.clothing.entity.ShiftConfig> updateShiftConfig(@PathVariable Integer id, @RequestBody com.sapo.mock.clothing.entity.ShiftConfig config) {
        return ResponseEntity.ok(shiftHandoverService.updateShiftConfig(id, config));
    }

    @DeleteMapping("/configs/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ApiMessage("Xóa ca làm việc thành công")
    public ResponseEntity<Void> deactivateShiftConfig(@PathVariable Integer id) {
        shiftHandoverService.deactivateShiftConfig(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    @ApiMessage("Lấy ca đang hoạt động thành công")
    public ResponseEntity<ShiftHandover> getActiveShift() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        ShiftHandover handover = shiftHandoverService.getActiveShift(username).orElse(null);
        return ResponseEntity.ok(handover);
    }

    @PostMapping("/open")
    @ApiMessage("Mở ca làm việc thành công")
    public ResponseEntity<ShiftHandover> openShift(@Valid @RequestBody OpenShiftRequestDTO dto) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        ShiftHandover handover = shiftHandoverService.openShift(username, dto.getShiftName(), dto.getInitialAmount());
        return ResponseEntity.ok(handover);
    }

    @PostMapping("/handover")
    @ApiMessage("Bàn giao ca thành công")
    public ResponseEntity<ShiftHandover> saveHandover(@Valid @RequestBody HandoverRequestDTO dto) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));

        ShiftHandover handover = shiftHandoverService.completeShift(
                username,
                dto.getInitialAmount(),
                dto.getActualAmount(),
                dto.getNote());
        return ResponseEntity.ok(handover);
    }

    @GetMapping("/latest-system")
    @ApiMessage("Lấy ca làm việc mới nhất toàn hệ thống")
    public ResponseEntity<ShiftHandover> getLatestSystemShift() {
        ShiftHandover handover = shiftHandoverService.getLatestSystemShift().orElse(null);
        return ResponseEntity.ok(handover);
    }

    @PutMapping("/update-open")
    @ApiMessage("Cập nhật thông tin ca đang mở thành công")
    public ResponseEntity<ShiftHandover> updateOpenShift(@Valid @RequestBody OpenShiftRequestDTO dto) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        ShiftHandover handover = shiftHandoverService.updateOpenShift(
                username,
                dto.getShiftName(),
                dto.getInitialAmount());
        return ResponseEntity.ok(handover);
    }

    @GetMapping("/system-amount")
    @ApiMessage("Lấy doanh thu hệ thống thành công")
    public ResponseEntity<BigDecimal> getSystemAmount() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        BigDecimal amount = shiftHandoverService.getUserRevenueToday(username);
        return ResponseEntity.ok(amount);
    }

    @GetMapping("/history")
    @ApiMessage("Lấy lịch sử bàn giao ca thành công")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_SHIFT', 'MANAGE_SHIFT')")
    public ResponseEntity<List<ShiftHandover>> getHandoverHistory() {
        // Trả về lịch sử bàn giao ca
        return ResponseEntity.ok(shiftHandoverService.getHandoverHistory());
    }

    @Getter
    @Setter
    public static class OpenShiftRequestDTO {
        @NotBlank(message = "Tên ca không được để trống")
        private String shiftName;

        private BigDecimal initialAmount;
    }

    @Getter
    @Setter
    public static class HandoverRequestDTO {
        private String shiftName;

        private BigDecimal initialAmount;

        private BigDecimal systemAmount;

        @NotNull(message = "Tiền mặt thực tế không được để trống")
        private BigDecimal actualAmount;

        private String note;
    }
}
