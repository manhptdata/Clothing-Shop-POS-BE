package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationHelper {

    private final NotificationService notificationService;

    public void sendOrderNotifications(Order savedOrder, String paymentMethod) {
        try {
            String payMethod = "QR_SEPAY".equals(paymentMethod) ? "Chuyển khoản" : "Tiền mặt";
            String customerName = savedOrder.getCustomerName();
            if (customerName == null || customerName.trim().isEmpty() || customerName.contains("Khách lẻ"))
                customerName = "Khách lẻ";

            DecimalFormat df = new DecimalFormat("#,###",
                    new DecimalFormatSymbols(Locale.US));
            String amountFormatted = df.format(savedOrder.getTotalAmount()) + " VND";

            Notification createNotif = new Notification();
            createNotif.setTitle("Đơn hàng mới");
            createNotif.setMessage(String.format("Đơn hàng %s được mua bởi %s qua nguồn đơn POS",
                    savedOrder.getOrderNumber(), customerName));
            createNotif.setType("ORDER_CREATED");
            createNotif.setTargetRole("ROLE_ADMIN");
            createNotif.setMetadata(String.format("{\"orderId\":%d,\"orderNumber\":\"%s\"}", savedOrder.getId(),
                    savedOrder.getOrderNumber()));
            notificationService.sendNotification(createNotif);

            Notification paidNotif = new Notification();
            paidNotif.setTitle("Thanh toán thành công");
            paidNotif.setMessage(String.format("Đơn hàng %s được thanh toán %s thành công bằng phương thức %s",
                    savedOrder.getOrderNumber(), amountFormatted, payMethod));
            paidNotif.setType("ORDER_PAID");
            paidNotif.setMetadata(String.format("{\"orderId\":%d,\"orderNumber\":\"%s\"}", savedOrder.getId(),
                    savedOrder.getOrderNumber()));
            notificationService.sendNotification(paidNotif);
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo đơn hàng mới: {}", e.getMessage(), e);
        }
    }

    public void sendPaymentFailureNotification(Order order, BigDecimal paidAmount) {
        try {
            DecimalFormat df = new DecimalFormat("#,###",
                    new DecimalFormatSymbols(Locale.US));
            String paidFormatted = df.format(paidAmount) + " VND";
            String totalFormatted = df.format(order.getTotalAmount()) + " VND";

            Notification failNotif = new Notification();
            failNotif.setTitle("Thanh toán thiếu tiền");
            failNotif.setMessage(
                    String.format("Cảnh báo: Đơn hàng %s chuyển thiếu tiền! Khách chuyển %s, cần thanh toán %s.",
                            order.getOrderNumber(), paidFormatted, totalFormatted));
            failNotif.setType("SYSTEM");
            failNotif.setMetadata(String.format(
                    "{\"orderNumber\":\"%s\",\"type\":\"PAYMENT_INSUFFICIENT\",\"paidAmount\":%s,\"totalAmount\":%s}",
                    order.getOrderNumber(), paidAmount.toString(), order.getTotalAmount().toString()));
            notificationService.sendNotification(failNotif);
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo thanh toán thiếu: {}", e.getMessage(), e);
        }
    }
}
