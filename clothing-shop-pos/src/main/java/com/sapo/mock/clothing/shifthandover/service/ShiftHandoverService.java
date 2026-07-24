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

import com.sapo.mock.clothing.returnorder.repository.ReturnOrderRepository;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository shiftHandoverRepository;
    private final com.sapo.mock.clothing.shifthandover.repository.ShiftConfigRepository shiftConfigRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final ReturnOrderRepository returnOrderRepository;
    private final com.sapo.mock.clothing.payment.repository.PaymentLogRepository paymentLogRepository;

    // --- QUẢN LÝ DANH MỤC CA LÀM VIỆC DÀNH CHO ADMIN ---
    public List<com.sapo.mock.clothing.entity.ShiftConfig> getActiveShiftConfigs() {
        return shiftConfigRepository.findByActiveTrue();
    }

    @Transactional
    public com.sapo.mock.clothing.entity.ShiftConfig createShiftConfig(com.sapo.mock.clothing.entity.ShiftConfig config) {
        config.setActive(true);
        return shiftConfigRepository.save(config);
    }

    @Transactional
    public com.sapo.mock.clothing.entity.ShiftConfig updateShiftConfig(Integer id, com.sapo.mock.clothing.entity.ShiftConfig updated) {
        com.sapo.mock.clothing.entity.ShiftConfig config = shiftConfigRepository.findById(id)
                .orElseThrow(() -> new com.sapo.mock.clothing.exception.BadRequestException("Không tìm thấy ca làm việc."));
        config.setName(updated.getName());
        config.setStartTime(updated.getStartTime());
        config.setEndTime(updated.getEndTime());
        return shiftConfigRepository.save(config);
    }

    @Transactional
    public void deactivateShiftConfig(Integer id) {
        com.sapo.mock.clothing.entity.ShiftConfig config = shiftConfigRepository.findById(id)
                .orElseThrow(() -> new com.sapo.mock.clothing.exception.BadRequestException("Không tìm thấy ca làm việc."));
        config.setActive(false);
        shiftConfigRepository.save(config);
    }

    // --- QUẢN LÝ BÀN GIAO CA LÀM VIỆC ---
    public BigDecimal getUserRevenueToday(String username) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant openedAtTime = shiftHandoverRepository
                .findFirstByCashierUsernameAndStatusOrderByCreatedAtDesc(username, "OPEN")
                .map(s -> s.getOpenedAt() != null ? s.getOpenedAt() : s.getCreatedAt())
                .orElseGet(() -> {
                    return shiftHandoverRepository.findTopByCashierUsernameOrderByCreatedAtDesc(username)
                            .map(s -> s.getOpenedAt() != null ? s.getOpenedAt() : s.getCreatedAt())
                            .filter(t -> t.isAfter(startOfDay))
                            .orElse(startOfDay);
                });
        BigDecimal netRevenue = orderRepository.calculateUserRevenueBetween(username, openedAtTime, Instant.now());
        if (netRevenue == null)
            netRevenue = BigDecimal.ZERO;
        return netRevenue;
    }

    public BigDecimal getUserTransferRevenueToday(String username) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant openedAtTime = shiftHandoverRepository
                .findFirstByCashierUsernameAndStatusOrderByCreatedAtDesc(username, "OPEN")
                .map(s -> s.getOpenedAt() != null ? s.getOpenedAt() : s.getCreatedAt())
                .orElseGet(() -> {
                    return shiftHandoverRepository.findTopByCashierUsernameOrderByCreatedAtDesc(username)
                            .map(s -> s.getOpenedAt() != null ? s.getOpenedAt() : s.getCreatedAt())
                            .filter(t -> t.isAfter(startOfDay))
                            .orElse(startOfDay);
                });
        BigDecimal netTransfer = orderRepository.calculateUserTransferRevenueBetween(username, openedAtTime, Instant.now());
        return netTransfer != null ? netTransfer : BigDecimal.ZERO;
    }

    public java.util.Optional<ShiftHandover> getActiveShift(String username) {
        return shiftHandoverRepository.findFirstByCashierUsernameAndStatusOrderByCreatedAtDesc(username, "OPEN");
    }

    @Transactional
    public ShiftHandover openShift(String username, String shiftName, BigDecimal initialAmount) {
        if (getActiveShift(username).isPresent()) {
            throw new com.sapo.mock.clothing.exception.BadRequestException(
                    "Bạn đang có một ca làm việc chưa kết thúc. Vui lòng kết ca trước.");
        }

        BigDecimal init = initialAmount != null ? initialAmount : BigDecimal.ZERO;
        ShiftHandover handover = new ShiftHandover();
        handover.setCashierUsername(username);
        handover.setShiftName(shiftName);
        handover.setInitialAmount(init);
        handover.setSystemAmount(BigDecimal.ZERO);
        handover.setActualAmount(BigDecimal.ZERO);
        handover.setDiscrepancy(BigDecimal.ZERO);
        handover.setStatus("OPEN");
        handover.setOpenedAt(Instant.now());

        return shiftHandoverRepository.save(handover);
    }

    @Transactional
    public ShiftHandover saveHandover(String cashierUsername, String shiftName, BigDecimal initialAmount,
            BigDecimal systemAmount, BigDecimal actualAmount, String note) {
        return completeShift(cashierUsername, initialAmount, actualAmount, note);
    }

    @Transactional
    public ShiftHandover completeShift(String cashierUsername, BigDecimal initialAmount, BigDecimal actualAmount,
            String note) {
        ShiftHandover activeShift = getActiveShift(cashierUsername)
                .orElseThrow(() -> new com.sapo.mock.clothing.exception.BadRequestException(
                        "Không tìm thấy ca làm việc đang mở để kết ca."));

        if (initialAmount != null) {
            activeShift.setInitialAmount(initialAmount);
        }

        BigDecimal systemAmount = getUserRevenueToday(cashierUsername);
        BigDecimal transferAmount = getUserTransferRevenueToday(cashierUsername);
        BigDecimal expectedAmount = activeShift.getInitialAmount().add(systemAmount);
        BigDecimal discrepancy = actualAmount.subtract(expectedAmount);

        activeShift.setSystemAmount(systemAmount);
        activeShift.setTransferAmount(transferAmount);
        activeShift.setActualAmount(actualAmount);
        activeShift.setDiscrepancy(discrepancy);
        activeShift.setNote(note);
        activeShift.setStatus("COMPLETED");
        activeShift.setClosedAt(Instant.now());

        ShiftHandover saved = shiftHandoverRepository.save(activeShift);

        // Gửi thông báo đến Admin và Manager
        Notification notification = new Notification();
        notification.setTitle(discrepancy.compareTo(BigDecimal.ZERO) != 0 ? "⚠️ Bàn giao ca (Lệch quỹ)" : "Bàn giao ca / Biến động két");
        notification.setType("SHIFT_HANDOVER");
        notification.setTargetRole("ROLE_ADMIN,ROLE_MANAGER");

        String diffText;
        if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
            diffText = "thừa " + discrepancy + "đ";
        } else if (discrepancy.compareTo(BigDecimal.ZERO) < 0) {
            diffText = "thiếu " + discrepancy.abs() + "đ";
        } else {
            diffText = "đúng két";
        }

        String msg = String.format(
                "Ca [%s] (NV: %s) kết ca. Tiền mặt: %,.0fđ | CK: %,.0fđ | Két đầu ca: %,.0fđ | Tiền đếm thực tế: %,.0fđ (%s).",
                activeShift.getShiftName(), cashierUsername, systemAmount, transferAmount, activeShift.getInitialAmount(), actualAmount,
                diffText);
        if (note != null && !note.trim().isEmpty()) {
            msg += " Ghi chú: " + note;
        }
        notification.setMessage(msg);

        // Tạo metadata
        notification
                .setMetadata(String.format("{\"handoverId\":%d,\"cashier\":\"%s\",\"shift\":\"%s\",\"discrepancy\":%s}",
                        saved.getId(), cashierUsername, activeShift.getShiftName(), discrepancy));

        notificationService.sendNotification(notification);

        return saved;
    }

    public List<ShiftHandover> getHandoverHistory() {
        return shiftHandoverRepository.findAllByOrderByCreatedAtDesc();
    }

    public java.util.Optional<ShiftHandover> getLatestSystemShift() {
        return shiftHandoverRepository.findFirstByOrderByCreatedAtDesc();
    }

    @Transactional
    public ShiftHandover updateOpenShift(String cashierUsername, String shiftName, BigDecimal initialAmount) {
        ShiftHandover activeShift = getActiveShift(cashierUsername)
                .orElseThrow(() -> new com.sapo.mock.clothing.exception.BadRequestException(
                        "Không tìm thấy ca làm việc đang mở để cập nhật."));

        if (shiftName != null && !shiftName.trim().isEmpty()) {
            activeShift.setShiftName(shiftName.trim());
        }
        if (initialAmount != null) {
            activeShift.setInitialAmount(initialAmount);
        }

        return shiftHandoverRepository.save(activeShift);
    }
}
