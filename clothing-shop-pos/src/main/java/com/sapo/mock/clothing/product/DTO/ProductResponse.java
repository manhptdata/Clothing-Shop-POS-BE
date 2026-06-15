package com.sapo.mock.clothing.product.DTO;

import java.time.LocalDateTime;
import java.util.List;

import com.sapo.mock.clothing.category.DTO.CategoryResponse;

import lombok.Data;

@Data
public class ProductResponse {
	private Integer id;

	private String name;

	private CategoryResponse category;

	private String description;

	private Integer lowStockThreshold = 5;

	private Boolean isDeleted = false;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private Integer updatedByUserID;

	private Integer createdByUserID;

	private List<String> imageUrls;

	private List<ProductAttributeResponse> attributes;

	private List<ProductVariantResponse> variants;
}
