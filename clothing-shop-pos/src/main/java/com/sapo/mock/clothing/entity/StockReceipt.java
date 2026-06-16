package com.sapo.mock.clothing.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import com.sapo.mock.clothing.util.constant.ReceiptStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_receipt")
public class StockReceipt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReceiptStatus status;

	@Column(columnDefinition = "TEXT")
	private String note;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "created_by")
//    private User createdBy;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "confirmed_by")
//    private User confirmedBy;

	@Column(name = "total_quantity", nullable = false)
	private Integer totalQuantity = 0;

	@OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StockReceiptItem> items = new ArrayList<>();

	@LastModifiedBy
	@Column(name = "confirmed_by")
	private Integer confirmedBy;

	@CreatedBy
	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = Instant.now();
	}
}
