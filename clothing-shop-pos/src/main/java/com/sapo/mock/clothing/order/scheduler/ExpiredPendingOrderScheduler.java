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

    /**
     * Chạy mỗi 15 phút. Quét các đơn PENDING được tạo cách đây hơn 30 phút.
     */
    @Scheduled(fixedRate = 900000) // 15 phút = 15 * 60 * 1000 ms
    public void cancelExpiredPendingOrders() {
        log.info("[ExpiredPendingOrderScheduler] Bắt đầu quét đơn hàng PENDING quá hạn...");
        
        Instant thresholdTime = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, thresholdTime);
        
        if (expiredOrders.isEmpty()) {
            log.info("[ExpiredPendingOrderScheduler] Không có đơn hàng PENDING nào quá hạn.");
            return;
        }

        ReqCancelOrderDTO cancelDto = new ReqCancelOrderDTO();
        cancelDto.setReason("Hệ thống tự động hủy do hết hạn thanh toán PENDING (quá 30 phút)");

        int count = 0;
        for (Order order : expiredOrders) {
            try {
                // Gọi qua orderService để kích hoạt cả logic hoàn Loyalty
                orderService.cancelOrder(order.getId(), cancelDto, "system");
                count++;
                log.info("Đã tự động hủy đơn hàng: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Lỗi khi tự động hủy đơn {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }
        
        log.info("[ExpiredPendingOrderScheduler] Hoàn tất quét. Đã hủy {}/{} đơn hàng.", count, expiredOrders.size());
    }
}
