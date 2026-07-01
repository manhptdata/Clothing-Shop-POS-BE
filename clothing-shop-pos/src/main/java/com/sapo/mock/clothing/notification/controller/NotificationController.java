package com.sapo.mock.clothing.notification.controller;

import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.notification.service.NotificationService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        return notificationService.subscribe(username);
    }

    @GetMapping
    @ApiMessage("Lấy danh sách thông báo thành công")
    public ResponseEntity<List<Notification>> getNotifications() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        List<Notification> notifications = notificationService.getNotificationsForUser(username);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    @ApiMessage("Đánh dấu đã đọc thông báo thành công")
    public ResponseEntity<Notification> markRead(@PathVariable Integer id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    @ApiMessage("Đánh dấu đã đọc tất cả thông báo thành công")
    public ResponseEntity<Void> markAllRead() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        notificationService.markAllAsRead(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approval-request")
    @ApiMessage("Gửi yêu cầu phê duyệt thành công")
    public ResponseEntity<Notification> createApprovalRequest(@RequestBody ApprovalRequestDTO dto) {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestException("Vui lòng đăng nhập"));
        
        if (dto.getOrderNumber() == null || dto.getOrderNumber().trim().isEmpty()) {
            throw new BadRequestException("Mã hóa đơn không được để trống");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new BadRequestException("Lý do không được để trống");
        }

        Notification requestNotif = notificationService.createApprovalRequest(dto.getOrderNumber(), dto.getReason(), username);
        return ResponseEntity.ok(requestNotif);
    }

    @PostMapping("/approval-request/{id}/approve")
    @ApiMessage("Phê duyệt yêu cầu thành công")
    public ResponseEntity<Notification> approveRequest(@PathVariable Integer id) {
        Notification approved = notificationService.approveRequest(id);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/approval-request/{id}/reject")
    @ApiMessage("Từ chối yêu cầu thành công")
    public ResponseEntity<Notification> rejectRequest(@PathVariable Integer id) {
        Notification rejected = notificationService.rejectRequest(id);
        return ResponseEntity.ok(rejected);
    }

    @Getter
    @Setter
    public static class ApprovalRequestDTO {
        private String orderNumber;
        private String reason;
    }
}
