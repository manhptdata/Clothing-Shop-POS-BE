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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_variant")
public class ProductVariant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, length = 50, unique = true)
	private String sku;

	@Column(name = "image_url")
	private String imageUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "option1_value_id")
	private ProductOptionValue option1Value;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "option2_value_id")
	private ProductOptionValue option2Value;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "option3_value_id")
	private ProductOptionValue option3Value;

	@Column(name = "sale_price", nullable = false)
	private BigDecimal salePrice;

	@Column(name = "import_price")
	private BigDecimal importPrice;

	@Column(name = "low_stock_threshold", nullable = false)
	private Integer lowStockThreshold = 5;

	@Column(name = "quantity", nullable = false)
	private Integer quantity = 0;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;
}