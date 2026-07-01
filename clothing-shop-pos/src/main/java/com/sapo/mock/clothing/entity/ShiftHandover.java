package com.sapo.mock.clothing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shift_handover")
public class ShiftHandover {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cashier_username", nullable = false)
    private String cashierUsername;

    @Column(name = "shift_name", nullable = false)
    private String shiftName; // e.g. Ca sáng, Ca chiều

    @Column(name = "system_amount", nullable = false)
    private BigDecimal systemAmount; // Doanh thu hệ thống tính toán

    @Column(name = "actual_amount", nullable = false)
    private BigDecimal actualAmount; // Tiền mặt thực tế đếm được

    @Column(name = "discrepancy", nullable = false)
    private BigDecimal discrepancy; // Chênh lệch (actual - system)

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
