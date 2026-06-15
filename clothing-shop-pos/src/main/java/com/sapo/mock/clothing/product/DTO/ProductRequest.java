package com.sapo.mock.clothing.product.DTO;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductRequest {
	private String sku;

	private String name;

	private Integer categoryId;

	private String color;

	private String size;

	private BigDecimal salePrice;

	private BigDecimal importPrice;

	private String description;

	private Integer lowStockThreshold;

	private List<String> imageUrls;

	private List<ProductAttributeRequest> attributes;

	private List<ProductVariantRequest> variants;
}
