package com.sapo.mock.clothing.entity;

import java.time.Instant;

import com.sapo.mock.clothing.util.constant.StockLogReferenceType;
import com.sapo.mock.clothing.util.constant.StockLogSource;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_log")
public class StockLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id", nullable = false)
	private ProductVariant variant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Column(name = "quantity_before", nullable = false)
	private int quantityBefore;

	@Column(name = "quantity_change", nullable = false)
	private int quantityChange;

	@Column(name = "quantity_after", nullable = false)
	private int quantityAfter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StockLogSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type", length = 20)
	private StockLogReferenceType referenceType;

	@Column(name = "reference_id")
	private Integer referenceId;

	@Column(columnDefinition = "TEXT")
	private String note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = Instant.now();
	}
}
