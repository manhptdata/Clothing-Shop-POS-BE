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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final ShiftHandoverService shiftHandoverService;

    @PostMapping("/handover")
    @ApiMessage("Bàn giao ca thành công")
    public ResponseEntity<ShiftHandover> saveHandover(@Valid @RequestBody HandoverRequestDTO dto) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));

        ShiftHandover handover = shiftHandoverService.saveHandover(
                username,
                dto.getShiftName(),
                dto.getSystemAmount(),
                dto.getActualAmount(),
                dto.getNote()
        );
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
    public ResponseEntity<List<ShiftHandover>> getHandoverHistory() {
        // Trả về lịch sử bàn giao ca
        return ResponseEntity.ok(shiftHandoverService.getHandoverHistory());
    }

    @Getter
    @Setter
    public static class HandoverRequestDTO {
        @NotBlank(message = "Tên ca không được để trống")
        private String shiftName;

        @NotNull(message = "Doanh thu hệ thống không được để trống")
        private BigDecimal systemAmount;

        @NotNull(message = "Tiền mặt thực tế không được để trống")
        private BigDecimal actualAmount;

        private String note;
    }
}
