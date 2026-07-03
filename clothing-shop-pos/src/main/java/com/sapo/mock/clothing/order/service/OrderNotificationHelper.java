package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###",
                    new java.text.DecimalFormatSymbols(java.util.Locale.US));
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
            System.err.println("Lỗi gửi thông báo đơn hàng mới: " + e.getMessage());
        }
    }

    public void sendPaymentFailureNotification(Order order, BigDecimal paidAmount) {
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###",
                    new java.text.DecimalFormatSymbols(java.util.Locale.US));
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
            System.err.println("Lỗi gửi thông báo thanh toán thiếu: " + e.getMessage());
        }
    }
}
