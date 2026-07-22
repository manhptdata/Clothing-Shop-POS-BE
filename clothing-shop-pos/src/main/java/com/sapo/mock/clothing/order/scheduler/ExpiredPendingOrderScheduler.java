package com.sapo.mock.clothing.order.scheduler;

import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.order.dto.ReqCancelOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.order.service.OrderService;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredPendingOrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final com.sapo.mock.clothing.setting.service.SystemSettingService systemSettingService;

    /**
     * Chạy mỗi 1 phút. Quét các đơn PENDING được tạo cách đây vượt quá số phút cấu hình.
     */
    @Scheduled(fixedRate = 60000) // 1 phút = 1 * 60 * 1000 ms
    public void cancelExpiredPendingOrders() {
        int timeoutMinutes = systemSettingService.getPendingOrderTimeoutMinutes();
        log.info("[ExpiredPendingOrderScheduler] Bắt đầu quét đơn hàng PENDING quá hạn (cấu hình {} phút)...", timeoutMinutes);
        
        Instant thresholdTime = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, thresholdTime);
        
        if (expiredOrders.isEmpty()) {
            log.info("[ExpiredPendingOrderScheduler] Không có đơn hàng PENDING nào quá hạn.");
            return;
        }

        ReqCancelOrderDTO cancelDto = new ReqCancelOrderDTO();
        String cancelReason = "Hệ thống tự động hủy do hết hạn thanh toán PENDING (quá " + timeoutMinutes + " phút)";
        cancelDto.setReason(cancelReason);

        int count = 0;
        for (Order order : expiredOrders) {
            try {
                // Gọi cancelExpiredSystemOrder để bỏ qua bước check PIN admin và bảo vệ đơn đã nạp cọc/thiếu tiền
                orderService.cancelExpiredSystemOrder(order.getId(), cancelReason);
                count++;
                log.info("Đã rà soát tự động đơn hàng: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Lỗi khi tự động rà soát hủy đơn {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }
        
        log.info("[ExpiredPendingOrderScheduler] Hoàn tất quét. Đã hủy {}/{} đơn hàng.", count, expiredOrders.size());
    }
}
