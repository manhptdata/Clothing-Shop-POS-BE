package com.sapo.mock.clothing.product.DTO;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductVariantResponse {
	private Integer id;
	private String sku;

	private String option1Value;
	private String option2Value;
	private String option3Value;

	private BigDecimal salePrice;
	private BigDecimal importPrice;
	private Integer lowStockThreshold;
	private Integer quantity;
	private String imageUrl;
	private Boolean isActive;
}