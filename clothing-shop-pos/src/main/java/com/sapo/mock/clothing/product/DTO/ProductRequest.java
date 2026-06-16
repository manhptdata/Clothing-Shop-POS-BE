package com.sapo.mock.clothing.product.DTO;

import java.util.List;

import lombok.Data;

@Data
public class ProductRequest {
	private String name;
	private Integer categoryId;
	private String description;
	private List<String> imageUrls;

	private List<ProductOptionRequest> options;
	private List<ProductAttributeRequest> attributes;
	private List<ProductVariantRequest> variants;
}