package com.sapo.mock.clothing.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_log")
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reference_code", unique = true, nullable = false, length = 100)
    private String referenceCode;

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Column(name = "transfer_amount", precision = 15, scale = 2)
    private BigDecimal transferAmount;

    @Column(name = "gateway", length = 50)
    private String gateway;

    @Column(name = "transaction_date", length = 50)
    private String transactionDate;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refunded_by", length = 50)
    private String refundedBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
