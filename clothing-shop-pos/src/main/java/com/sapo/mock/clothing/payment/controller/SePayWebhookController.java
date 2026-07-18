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
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("HD-?\\d{8}-?\\d{3,}");

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
            
            // Check idempotency
            if (request.getReferenceCode() != null && paymentLogRepository.existsByReferenceCode(request.getReferenceCode())) {
                log.info("Webhook trùng lặp (referenceCode={}), bỏ qua.", request.getReferenceCode());
                return ResponseEntity.ok(Map.of("success", true));
            }

            String content = request.getContent() != null ? request.getContent() : "";
            Matcher matcher = ORDER_NUMBER_PATTERN.matcher(content);

            if (matcher.find()) {
                String matchedStr = matcher.group();
                String orderNumber = matchedStr;

                // Nếu nội dung bị ngân hàng lọc bỏ dấu gạch ngang
                if (!matchedStr.contains("-") && matchedStr.length() >= 13) {
                    String datePart = matchedStr.substring(2, 10);
                    String seqPart = matchedStr.substring(10);
                    orderNumber = "HD-" + datePart + "-" + seqPart;
                }

                try {
                    orderService.completeOrderPayment(orderNumber, request.getTransferAmount());
                    log.info("Thanh toán thành công cho đơn hàng: {}", orderNumber);
                    savePaymentLog(request, orderNumber, "SUCCESS");
                } catch (BadRequestException e) {
                    // Lỗi nghiệp vụ (chuyển thiếu tiền, đơn không pending)
                    log.warn("Lỗi nghiệp vụ xử lý thanh toán đơn hàng {}: {}", orderNumber, e.getMessage());
                    savePaymentLog(request, orderNumber, "INSUFFICIENT");
                } catch (Exception e) {
                    // Lỗi hệ thống (DB lỗi, timeout) -> Trả 500 để SePay retry
                    log.error("Lỗi hệ thống xử lý thanh toán đơn hàng {}: {}", orderNumber, e.getMessage(), e);
                    savePaymentLog(request, orderNumber, "ERROR");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("success", false));
                }
            } else {
                log.info("Không tìm thấy mã đơn hàng trong nội dung chuyển khoản.");
                savePaymentLog(request, null, "NO_ORDER");
            }
        }

        // SePay yêu cầu trả về {"success": true} và mã HTTP 200
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void savePaymentLog(SePayWebhookRequest request, String orderNumber, String status) {
        try {
            PaymentLog paymentLog = new PaymentLog();
            paymentLog.setReferenceCode(request.getReferenceCode());
            paymentLog.setOrderNumber(orderNumber);
            paymentLog.setTransferAmount(request.getTransferAmount());
            paymentLog.setGateway(request.getGateway());
            paymentLog.setTransactionDate(request.getTransactionDate());
            paymentLog.setContent(request.getContent());
            paymentLog.setStatus(status);
            paymentLogRepository.save(paymentLog);
        } catch (Exception e) {
            log.error("Lỗi khi lưu PaymentLog cho referenceCode={}: {}", request.getReferenceCode(), e.getMessage());
        }
    }
}
