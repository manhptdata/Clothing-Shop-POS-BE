package com.sapo.mock.clothing.payment.controller;

import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.entity.SystemSetting;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.setting.service.SystemSettingService;
import com.sapo.mock.clothing.setting.service.impl.SystemSettingServiceImpl;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.sapo.mock.clothing.payment.repository.PaymentLogRepository;
import com.sapo.mock.clothing.entity.PaymentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.sapo.mock.clothing.common.dto.response.RestResponse;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final SystemSettingService systemSettingService;
    private final OrderRepository orderRepository;
    private final PaymentLogRepository paymentLogRepository;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_ORDER')")
    public ResponseEntity<RestResponse<Page<PaymentLog>>> getPaymentLogs(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        org.springframework.data.jpa.domain.Specification<PaymentLog> spec = 
            com.sapo.mock.clothing.specification.PaymentLogSpecification.filterLogs(orderNumber, status, gateway, startDate, endDate);

        Page<PaymentLog> result = paymentLogRepository.findAll(
            spec,
            PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by("createdAt").descending())
        );
        RestResponse<Page<PaymentLog>> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setMessage("Lấy lịch sử thanh toán thành công");
        response.setData(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr-code")
    @ApiMessage("Lấy thông tin mã QR thanh toán thành công")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getQrCode(
            @RequestParam String orderNumber) {

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Đơn hàng không ở trạng thái chờ thanh toán");
        }

        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal amount = order.getTotalAmount().subtract(paidAmount);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán đủ");
        }
        SystemSetting bankNameSetting = systemSettingService.getSettingByKey(SystemSettingServiceImpl.SETTING_PAYMENT_BANK_NAME);
        SystemSetting bankAccountSetting = systemSettingService.getSettingByKey(SystemSettingServiceImpl.SETTING_PAYMENT_BANK_ACCOUNT);
        SystemSetting accountNameSetting = systemSettingService.getSettingByKey(SystemSettingServiceImpl.SETTING_PAYMENT_ACCOUNT_NAME);

        String bankName = (bankNameSetting != null && bankNameSetting.getSettingValue() != null) 
                ? bankNameSetting.getSettingValue() : "MBBank";
        String bankAccount = (bankAccountSetting != null && bankAccountSetting.getSettingValue() != null) 
                ? bankAccountSetting.getSettingValue() : "SBSEPAY";
        String accountName = (accountNameSetting != null && accountNameSetting.getSettingValue() != null) 
                ? accountNameSetting.getSettingValue() : "SHOP QUAN AO";

        String qrUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                bankAccount, bankName, amount.toPlainString(), orderNumber);

        Map<String, String> response = new HashMap<>();
        response.put("qrUrl", qrUrl);
        response.put("bankName", bankName);
        response.put("bankAccount", bankAccount);
        response.put("accountName", accountName);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logs/{id}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @ApiMessage("Xác nhận hoàn tiền thừa/trùng lặp thành công")
    public ResponseEntity<Void> refundPaymentLog(
            @PathVariable Integer id,
            @RequestParam(required = false) BigDecimal refundAmount) {

        PaymentLog log = paymentLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy log giao dịch"));

        if (!"DUPLICATE_PAYMENT".equals(log.getStatus()) && !"OVERPAID".equals(log.getStatus()) && !"INSUFFICIENT".equals(log.getStatus())) {
            throw new BadRequestException("Chỉ có thể hoàn tiền cho giao dịch chuyển trùng, chuyển thừa hoặc chuyển thiếu");
        }

        log.setStatus("REFUNDED");

        String amountStr = refundAmount != null ? refundAmount.toPlainString() : "Toàn bộ";
        String extraNote = " | [Đã hoàn tiền mặt: " + amountStr + " VND]";
        log.setContent(log.getContent() != null ? log.getContent() + extraNote : extraNote);

        paymentLogRepository.save(log);
        return ResponseEntity.ok().build();
    }
}
