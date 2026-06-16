package com.sapo.mock.clothing.product.DTO;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductVariantRequest {
	private String sku;
	// Xóa color và size, thay bằng 3 option:
	private String option1Value;
	private String option2Value;
	private String option3Value;

	private BigDecimal salePrice;
	private BigDecimal importPrice;
	private Integer lowStockThreshold;
	private String imageUrl;
}