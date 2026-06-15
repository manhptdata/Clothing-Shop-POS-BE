package com.sapo.mock.clothing.product.DTO;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductVariantRequest {
	private String sku;
	private String color;
	private String size;
	private BigDecimal salePrice;
	private BigDecimal importPrice;
	private Integer lowStockThreshold;
}
