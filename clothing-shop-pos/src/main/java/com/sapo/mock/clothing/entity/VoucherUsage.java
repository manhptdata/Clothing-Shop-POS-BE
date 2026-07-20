package com.sapo.mock.clothing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voucher_usage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"customer_id", "voucher_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "voucher_code", nullable = false, length = 50)
    private String voucherCode;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;
}
