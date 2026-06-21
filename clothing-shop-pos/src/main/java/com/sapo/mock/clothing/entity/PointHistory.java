package com.sapo.mock.clothing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "point_history")
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "order_id")
    private Integer orderId; // Có thể null nếu cộng điểm thủ công

    @Column(name = "points_change", nullable = false)
    private Integer pointsChange; // Số điểm thay đổi (+ hoặc -)

    @Column(length = 20, nullable = false)
    private String type; // EARN (Tích điểm), REDEEM (Tiêu điểm), REFUND (Hoàn điểm)

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
