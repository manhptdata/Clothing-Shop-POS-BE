package com.sapo.mock.clothing.entity;

import com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_voucher")
public class CustomerVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CustomerVoucherStatusEnum status = CustomerVoucherStatusEnum.UNUSED;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "order_id")
    private Integer orderId;
}