package com.sapo.mock.clothing.payment.controller;

import com.sapo.mock.clothing.order.service.OrderService;
import com.sapo.mock.clothing.payment.dto.SePayWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class SePayWebhookController {

    private final OrderService orderService;
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("HD-?\\d{8}-?\\d{3,}");

    @Value("${sepay.webhook.token}")
    private String webhookToken;

    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        System.out.println("Received SePay Webhook: " + request);

        // Kiểm tra Token bảo mật (Apikey hoặc Bearer Token)
        String expectedApikeyHeader = "Apikey " + webhookToken;
        String expectedBearerHeader = "Bearer " + webhookToken;
        if (authHeader == null
                || (!authHeader.equals(expectedApikeyHeader) && !authHeader.equals(expectedBearerHeader))) {
            System.err.println("Cảnh báo: Yêu cầu Webhook không hợp lệ hoặc thiếu Token bảo mật.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        // Chuyển khoản tiền vào
        if ("in".equalsIgnoreCase(request.getTransferType())) {
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
                    System.out.println("Thanh toán thành công cho đơn hàng: " + orderNumber);
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý thanh toán đơn hàng " + orderNumber + ": " + e.getMessage());
                }
            } else {
                System.out.println("Không tìm thấy mã đơn hàng trong nội dung chuyển khoản.");
            }
        }

        // SePay yêu cầu trả về {"success": true} và mã HTTP 200
        return ResponseEntity.ok(Map.of("success", true));
    }
}
