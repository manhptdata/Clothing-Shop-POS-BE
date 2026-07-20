package com.sapo.mock.clothing.voucher.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.request.VoucherRequest;
import com.sapo.mock.clothing.customer.dto.response.VoucherResponse;
import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import com.sapo.mock.clothing.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<RestResponse<List<VoucherResponse>>> getAllVouchers(@RequestParam(required = false) VoucherCampaignStatusEnum status) {
        List<VoucherResponse> vouchers = voucherService.getAllVouchers(status);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Lấy danh sách voucher thành công", vouchers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<VoucherResponse>> getVoucherById(@PathVariable Integer id) {
        VoucherResponse voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Lấy chi tiết voucher thành công", voucher));
    }

    @PostMapping
    public ResponseEntity<RestResponse<VoucherResponse>> createVoucher(@Valid @RequestBody VoucherRequest request) {
        VoucherResponse response = voucherService.createVoucher(request);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Tạo voucher thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<VoucherResponse>> updateVoucher(@PathVariable Integer id, @Valid @RequestBody VoucherRequest request) {
        VoucherResponse response = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Cập nhật voucher thành công", response));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RestResponse<VoucherResponse>> toggleVoucherStatus(@PathVariable Integer id) {
        VoucherResponse response = voucherService.toggleVoucherStatus(id);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Thay đổi trạng thái voucher thành công", response));
    }
}
