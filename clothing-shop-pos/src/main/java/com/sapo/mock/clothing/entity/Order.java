package com.sapo.mock.clothing.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.util.constant.PaymentMethod;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;

	@Column(name = "customer_id", nullable = false)
	private Integer customerId;


	@Column(name = "created_by", nullable = false)
	private Integer createdBy;

	@Column(name = "customer_name", length = 100)
	private String customerName;


	@Column(name = "created_by_username", length = 50)
	private String createdByUsername;

	@Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "change_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal changeAmount;

	@Column(name = "points_used", nullable = false)
	private Integer pointsUsed = 0;

	@Column(name = "points_earned", nullable = false)
	private Integer pointsEarned = 0;

	@Column(name = "discount_from_points", nullable = false, precision = 15, scale = 2)
	private BigDecimal discountFromPoints = BigDecimal.ZERO;

	@Column(name = "voucher_code", length = 50)
	private String voucherCode;

	@Column(name = "discount_from_voucher", nullable = false, precision = 15, scale = 2)
	private BigDecimal discountFromVoucher = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.COMPLETED;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", length = 20)
	private PaymentMethod paymentMethod;

	@Column(name = "is_printed", nullable = false)
	private boolean isPrinted = false;

	@Column(columnDefinition = "TEXT")
	private String note;

	@Column(name = "cancel_reason", length = 255)
	private String cancelReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OrderLineItem> items;

	@PrePersist
	public void prePersist() {
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
