package com.sapo.mock.clothing.entity;

<<<<<<< HEAD
=======
import com.sapo.mock.clothing.util.constant.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

>>>>>>> 26a7a8d4a8b1b309f03818e7a7f2ab6e3126f428
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

<<<<<<< HEAD
import com.sapo.mock.clothing.util.constant.InvoiceStatus;

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

=======
>>>>>>> 26a7a8d4a8b1b309f03818e7a7f2ab6e3126f428
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

<<<<<<< HEAD
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;

	@Column(name = "customer_id", nullable = false)
	private Integer customerId;

	@Column(name = "warehouse_id", nullable = false)
	private Integer warehouseId;

	@Column(name = "created_by", nullable = false)
	private Integer createdBy;

	@Column(name = "customer_name", length = 100)
	private String customerName;

	@Column(name = "warehouse_name", length = 150)
	private String warehouseName;

	@Column(name = "created_by_username", length = 50)
	private String createdByUsername;

	@Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "change_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal changeAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvoiceStatus status = InvoiceStatus.COMPLETED;

	@Column(name = "is_printed", nullable = false)
	private boolean isPrinted = false;

	@Column(columnDefinition = "TEXT")
	private String note;

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
=======
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "warehouse_name", length = 150)
    private String warehouseName;

    @Column(name = "created_by_username", length = 50)
    private String createdByUsername;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "change_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.COMPLETED;

    @Column(name = "is_printed", nullable = false)
    private boolean isPrinted = false;

    @Column(columnDefinition = "TEXT")
    private String note;

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
>>>>>>> 26a7a8d4a8b1b309f03818e7a7f2ab6e3126f428
