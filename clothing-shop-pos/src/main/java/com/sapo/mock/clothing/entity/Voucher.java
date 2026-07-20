package com.sapo.mock.clothing.entity;


import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private com.sapo.mock.clothing.util.constant.VoucherDiscountType discountType = com.sapo.mock.clothing.util.constant.VoucherDiscountType.FIXED_AMOUNT;

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value")
    private BigDecimal minOrderValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private VoucherCampaignStatusEnum status = VoucherCampaignStatusEnum.ACTIVE;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "used_quantity", nullable = false)
    private Integer usedQuantity = 0;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity = 0;

    @Column(name = "max_usage_per_user")
    private Integer maxUsagePerUser = 1;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "target_customer_group_id")
    private Integer targetCustomerGroupId;

    @Column(name = "apply_type", length = 20, nullable = false)
    private String applyType = "ALL";

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}