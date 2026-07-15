package com.sapo.mock.clothing.entity;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
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
@Table(name = "customer_group")
public class CustomerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name; // Tên nhóm khách hàng (Ví dụ: Khách Vùng A - Thường xuyên)

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CustomerStatusEnum status = CustomerStatusEnum.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 50, nullable = false)
    private RankCodeEnum code;

    private BigDecimal minSpending; // Chi tiêu tối thiểu

    // Voucher sinh nhật dành cho hạng này (chủ cửa hàng tự chọn từ danh sách voucher đã tạo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "birthday_voucher_id")
    private com.sapo.mock.clothing.entity.Voucher birthdayVoucher;

    // Tự động sinh thời gian khi tạo mới nhóm
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = CustomerStatusEnum.ACTIVE;
        }
    }
    // Tự động cập nhật thời gian khi có hành động Sửa hoặc Khóa nhóm
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}