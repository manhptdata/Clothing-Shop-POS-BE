package com.sapo.mock.clothing.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, unique = true, length = 50)
	private String sku;

	@Column(length = 50)
	private String color;

	@Column(length = 20)
	private String size;

	@Column(name = "sale_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "import_price", precision = 15, scale = 2)
	private BigDecimal importPrice;

	@Column(name = "low_stock_threshold", nullable = false)
	private Integer lowStockThreshold = 5;
}