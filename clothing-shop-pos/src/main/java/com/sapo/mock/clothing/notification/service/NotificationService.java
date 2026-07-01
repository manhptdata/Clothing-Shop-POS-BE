package com.sapo.mock.clothing.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.notification.repository.NotificationRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.order.service.OrderService;
import com.sapo.mock.clothing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private OrderService orderService;

    // Lưu các subscriber đang kết nối
    private final List<SseSubscriber> subscribers = new CopyOnWriteArrayList<>();

    private static class SseSubscriber {
        final SseEmitter emitter;
        final String username;
        final String role;
        final Integer userId;

        SseSubscriber(SseEmitter emitter, String username, String role, Integer userId) {
            this.emitter = emitter;
            this.username = username;
            this.role = role;
            this.userId = userId;
        }
    }

    /**
     * Đăng ký kết nối SSE cho user
     */
    public SseEmitter subscribe(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        }

        // Tạo emitter với timeout 1 giờ
        SseEmitter emitter = new SseEmitter(3600000L);
        SseSubscriber subscriber = new SseSubscriber(emitter, username, user.getRole().name(), user.getId());
        subscribers.add(subscriber);

        emitter.onCompletion(() -> subscribers.remove(subscriber));
        emitter.onTimeout(() -> subscribers.remove(subscriber));
        emitter.onError((e) -> subscribers.remove(subscriber));

        // Gửi event kết nối thành công ban đầu
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("Connected successfully for user: " + username));
        } catch (IOException e) {
            subscribers.remove(subscriber);
        }

        return emitter;
    }

    /**
     * Lưu thông báo vào DB và đẩy tới các clients qua SSE
     */
    @Transactional
    public Notification sendNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);

        List<SseSubscriber> deadSubscribers = new ArrayList<>();
        for (SseSubscriber subscriber : subscribers) {
            boolean shouldSend = false;

            if (saved.getTargetUserId() != null) {
                shouldSend = saved.getTargetUserId().equals(subscriber.userId);
            } else if (saved.getTargetRole() != null) {
                // Riêng LOW_STOCK gửi cho cả ADMIN và WH
                if ("LOW_STOCK".equals(saved.getType())) {
                    shouldSend = "ROLE_ADMIN".equals(subscriber.role) || "ROLE_WH".equals(subscriber.role);
                } else {
                    shouldSend = saved.getTargetRole().equals(subscriber.role);
                }
            } else {
                // Broadcast
                shouldSend = true;
            }

            if (shouldSend) {
                try {
                    subscriber.emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(String.valueOf(saved.getId()))
                            .data(saved));
                } catch (IOException e) {
                    deadSubscribers.add(subscriber);
                }
            }
        }
        subscribers.removeAll(deadSubscribers);
        return saved;
    }

    /**
     * Lấy danh sách thông báo hoạt động cho user
     */
    public List<Notification> getNotificationsForUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        }
        return notificationRepository.findActiveNotificationsForUser(user.getId(), user.getRole().name());
    }

    /**
     * Đánh dấu đã đọc
     */
    @Transactional
    public Notification markAsRead(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo ID: " + id));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    /**
     * Đánh dấu đọc tất cả của user hiện tại
     */
    @Transactional
    public void markAllAsRead(String username) {
        List<Notification> notifications = getNotificationsForUser(username);
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * Gửi yêu cầu phê duyệt hủy đơn
     */
    @Transactional
    public Notification createApprovalRequest(String orderNumber, String reason, String requestedByUsername) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));

        if (order.getStatus() == com.sapo.mock.clothing.util.constant.OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng này đã bị hủy trước đó.");
        }

        Notification notification = new Notification();
        notification.setTitle("Yêu cầu phê duyệt HỦY đơn");
        notification.setMessage(String.format("Nhân viên [%s] yêu cầu phê duyệt HỦY hóa đơn %s. Lý do: %s",
                requestedByUsername, orderNumber, reason));
        notification.setType("APPROVAL_REQUEST");
        notification.setTargetRole("ROLE_ADMIN");

        // Tạo metadata
        String metadataStr = String.format("{\"orderNumber\":\"%s\",\"type\":\"CANCEL_ORDER\",\"requestedBy\":\"%s\",\"reason\":\"%s\"}",
                orderNumber, requestedByUsername, reason);
        notification.setMetadata(metadataStr);

        return sendNotification(notification);
    }

    /**
     * Admin phê duyệt yêu cầu hủy đơn
     */
    @Transactional
    public Notification approveRequest(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo ID: " + notificationId));

        if (!"APPROVAL_REQUEST".equals(notification.getType())) {
            throw new BadRequestException("Thông báo này không phải là yêu cầu phê duyệt.");
        }

        try {
            JsonNode node = objectMapper.readTree(notification.getMetadata());
            String orderNumber = node.get("orderNumber").asText();
            String type = node.get("type").asText();

            if ("CANCEL_ORDER".equals(type)) {
                Order order = orderRepository.findByOrderNumber(orderNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));

                // Thực hiện hủy đơn trong database
                orderService.cancelOrder(order.getId());

                // Cập nhật thông báo
                notification.setTitle("Đã phê duyệt HỦY đơn");
                notification.setMessage(notification.getMessage() + " (Đã phê duyệt bởi Admin)");
                notification.setRead(true);
                notificationRepository.save(notification);

                // Gửi thông báo phản hồi thời gian thực (broadcast cho tất cả)
                Notification feedback = new Notification();
                feedback.setTitle("Hóa đơn đã được hủy");
                feedback.setMessage(String.format("Yêu cầu hủy hóa đơn %s đã được phê duyệt thành công.", orderNumber));
                feedback.setType("SYSTEM");
                sendNotification(feedback);

                return notification;
            }
        } catch (Exception e) {
            throw new BadRequestException("Lỗi xử lý phê duyệt: " + e.getMessage());
        }

        throw new BadRequestException("Loại yêu cầu không hợp lệ.");
    }

    /**
     * Admin từ chối yêu cầu hủy đơn
     */
    @Transactional
    public Notification rejectRequest(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo ID: " + notificationId));

        if (!"APPROVAL_REQUEST".equals(notification.getType())) {
            throw new BadRequestException("Thông báo này không phải là yêu cầu phê duyệt.");
        }

        try {
            JsonNode node = objectMapper.readTree(notification.getMetadata());
            String orderNumber = node.get("orderNumber").asText();

            // Cập nhật thông báo
            notification.setTitle("Đã từ chối HỦY đơn");
            notification.setMessage(notification.getMessage() + " (Bị từ chối bởi Admin)");
            notification.setRead(true);
            notificationRepository.save(notification);

            // Gửi thông báo phản hồi thời gian thực
            Notification feedback = new Notification();
            feedback.setTitle("Yêu cầu hủy đơn bị từ chối");
            feedback.setMessage(String.format("Yêu cầu hủy hóa đơn %s đã bị quản lý từ chối.", orderNumber));
            feedback.setType("SYSTEM");
            sendNotification(feedback);

            return notification;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi xử lý từ chối: " + e.getMessage());
        }
    }
}
