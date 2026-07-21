package com.sapo.mock.clothing.shifthandover.service;

import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.ShiftHandover;
import com.sapo.mock.clothing.notification.service.NotificationService;
import com.sapo.mock.clothing.shifthandover.repository.ShiftHandoverRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository shiftHandoverRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final com.sapo.mock.clothing.payment.repository.PaymentLogRepository paymentLogRepository;

    public BigDecimal getUserRevenueToday(String username) {
        Instant start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal orderRevenue = orderRepository.calculateUserRevenueToday(username, start);
        BigDecimal sepayRefunds = paymentLogRepository.calculateTotalRefundByUsernameSince(username, start);
        return orderRevenue.subtract(sepayRefunds);
    }

    @Transactional
    public ShiftHandover saveHandover(String cashierUsername, String shiftName, BigDecimal systemAmount, BigDecimal actualAmount, String note) {
        BigDecimal discrepancy = actualAmount.subtract(systemAmount);

        ShiftHandover handover = new ShiftHandover();
        handover.setCashierUsername(cashierUsername);
        handover.setShiftName(shiftName);
        handover.setSystemAmount(systemAmount);
        handover.setActualAmount(actualAmount);
        handover.setDiscrepancy(discrepancy);
        handover.setNote(note);

        ShiftHandover saved = shiftHandoverRepository.save(handover);

        // Gửi thông báo đến Admin
        Notification notification = new Notification();
        notification.setTitle("Bàn giao ca / Biến động két");
        notification.setType("SHIFT_HANDOVER");
        notification.setTargetRole("ROLE_ADMIN");

        String diffText;
        if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
            diffText = String.format("Thừa %,.0fđ", discrepancy);
        } else if (discrepancy.compareTo(BigDecimal.ZERO) < 0) {
            diffText = String.format("Thiếu %,.0fđ", discrepancy.abs());
        } else {
            diffText = "Khớp 100%";
        }

        String msg = String.format("Ca [%s] (Nhân viên: %s) đã kết ca. Kết quả két: %s so với doanh thu hệ thống (Thực tế: %,.0fđ | Hệ thống: %,.0fđ).",
                shiftName, cashierUsername, diffText, actualAmount, systemAmount);
        if (note != null && !note.trim().isEmpty()) {
            msg += " Ghi chú: " + note;
        }
        notification.setMessage(msg);

        // Tạo metadata
        notification.setMetadata(String.format("{\"handoverId\":%d,\"cashier\":\"%s\",\"shift\":\"%s\",\"discrepancy\":%s}",
                saved.getId(), cashierUsername, shiftName, discrepancy));

        notificationService.sendNotification(notification);

        return saved;
    }

    public List<ShiftHandover> getHandoverHistory() {
        return shiftHandoverRepository.findAllByOrderByCreatedAtDesc();
    }
}
