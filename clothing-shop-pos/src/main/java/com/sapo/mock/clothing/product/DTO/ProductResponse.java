package com.sapo.mock.clothing.product.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ProductResponse {
	private Integer id;

	private String sku;

	private String name;

	private String category;

	private String color;

	private String size;

	private BigDecimal salePrice;

	private BigDecimal importPrice;

	private String description;

	private List<String> imageUrls;

	private Integer lowStockThreshold = 5;

	private Boolean isDeleted = false;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private Integer updatedByUserID;

	private List<ProductAttributeResponse> attributes;
}
