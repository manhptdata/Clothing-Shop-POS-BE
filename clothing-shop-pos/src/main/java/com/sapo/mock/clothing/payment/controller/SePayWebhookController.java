package com.sapo.mock.clothing.payment.controller;

import com.sapo.mock.clothing.entity.PaymentLog;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.order.service.OrderService;
import com.sapo.mock.clothing.payment.dto.SePayWebhookRequest;
import com.sapo.mock.clothing.payment.repository.PaymentLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class SePayWebhookController {

    private final OrderService orderService;
    private final PaymentLogRepository paymentLogRepository;
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("\\bHD-?\\d{8}-?\\d{3,}\\b");

    @Value("${sepay.webhook.token}")
    private String webhookToken;

    @PostConstruct
    public void validateConfig() {
        if (webhookToken == null || webhookToken.isBlank()) {
            log.error("⚠️ SEPAY_WEBHOOK_TOKEN chưa được cấu hình! Webhook sẽ từ chối mọi request.");
        }
    }

    // link hứng dữ liệu từ sepay->dùng để dán vào config của sepay
    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        log.info("Received SePay Webhook: referenceCode={}, amount={}, type={}",
                request.getReferenceCode(), request.getTransferAmount(), request.getTransferType());

        // Kiểm tra Token bảo mật (Apikey hoặc Bearer Token)
        if (webhookToken == null || webhookToken.isBlank()) {
            log.error("⚠️ SEPAY_WEBHOOK_TOKEN chưa được cấu hình! Từ chối xử lý Webhook.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Webhook token is not configured"));
        }

        String expectedApikeyHeader = "Apikey " + webhookToken;
        String expectedBearerHeader = "Bearer " + webhookToken;
        if (authHeader == null
                || (!authHeader.equals(expectedApikeyHeader) && !authHeader.equals(expectedBearerHeader))) {
            log.warn("Cảnh báo: Yêu cầu Webhook không hợp lệ hoặc thiếu Token bảo mật.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        // Chuyển khoản tiền vào
        if ("in".equalsIgnoreCase(request.getTransferType())) {
            
            // 1. Ghi PaymentLog PROCESSING trước (atomic check)
            if (request.getReferenceCode() != null) {
                java.util.Optional<PaymentLog> existingLog = paymentLogRepository.findByReferenceCode(request.getReferenceCode());
                if (existingLog.isPresent()) {
                    String existingStatus = existingLog.get().getStatus();
                    if ("SUCCESS".equals(existingStatus) || "PROCESSING".equals(existingStatus) || "INSUFFICIENT".equals(existingStatus)) {
                        log.info("Webhook trùng lặp (referenceCode={}, status={}), bỏ qua.", request.getReferenceCode(), existingStatus);
                        return ResponseEntity.ok(Map.of("success", true));
                    }
                    // Status = ERROR hoặc NO_ORDER -> cho phép xử lý lại
                    paymentLogRepository.updateByReferenceCode(request.getReferenceCode(), "PROCESSING", null);
                } else {
                    try {
                        savePaymentLog(request, null, "PROCESSING");
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.info("Webhook trùng lặp (referenceCode={}) do concurrent request, bỏ qua.", request.getReferenceCode());
                        return ResponseEntity.ok(Map.of("success", true));
                    }
                }
            }

            String content = request.getContent() != null ? request.getContent() : "";
            Matcher matcher = ORDER_NUMBER_PATTERN.matcher(content);

            if (matcher.find()) {
                String matchedStr = matcher.group();
                String orderNumber;

                // Normalize: bỏ hết dấu gạch ngang -> rebuild chuẩn
                String normalized = matchedStr.replace("-", "");
                // normalized = "HD" + 8 số ngày + N số sequence
                if (normalized.length() >= 13) {
                    String datePart = normalized.substring(2, 10);
                    String seqPart = normalized.substring(10);
                    orderNumber = "HD-" + datePart + "-" + seqPart;
                } else {
                    orderNumber = matchedStr; // không đủ dài -> giữ nguyên
                }

                try {
                    String status = orderService.completeOrderPayment(orderNumber, request.getTransferAmount());
                    log.info("Thanh toán {} cho đơn hàng: {}", status, orderNumber);
                    paymentLogRepository.updateByReferenceCode(request.getReferenceCode(), status, orderNumber);
                } catch (com.sapo.mock.clothing.exception.DuplicatePaymentException e) {
                    log.warn("Thanh toán trùng lặp cho đơn hàng {}: {}", orderNumber, e.getMessage());
                    paymentLogRepository.updateByReferenceCode(request.getReferenceCode(), "DUPLICATE_PAYMENT", orderNumber);
                } catch (Exception e) {
                    // Lỗi hệ thống (DB lỗi, timeout) -> Trả 500 để SePay retry
                    log.error("Lỗi hệ thống xử lý thanh toán đơn hàng {}: {}", orderNumber, e.getMessage(), e);
                    paymentLogRepository.updateByReferenceCode(request.getReferenceCode(), "ERROR", orderNumber);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("success", false));
                }
            } else {
                log.info("Không tìm thấy mã đơn hàng trong nội dung chuyển khoản.");
                paymentLogRepository.updateByReferenceCode(request.getReferenceCode(), "NO_ORDER", null);
            }
        }

        // SePay yêu cầu trả về {"success": true} và mã HTTP 200
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void savePaymentLog(SePayWebhookRequest request, String orderNumber, String status) {
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setReferenceCode(request.getReferenceCode());
        paymentLog.setOrderNumber(orderNumber);
        paymentLog.setTransferAmount(request.getTransferAmount());
        paymentLog.setGateway(request.getGateway());
        paymentLog.setTransactionDate(request.getTransactionDate());
        paymentLog.setContent(request.getContent());
        paymentLog.setStatus(status);
        paymentLogRepository.save(paymentLog);
    }
}
