package com.sapo.mock.clothing.product.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ProductVariantRequest {
	private String sku;
	// Xóa color và size, thay bằng 3 option:
	private String option1Value;
	private String option2Value;
	private String option3Value;

	@Min(value = 0, message = "Giá bán lẻ không được nhỏ hơn 0")
	private BigDecimal salePrice;

	@Min(value = 0, message = "Giá vốn không được nhỏ hơn 0")
	private BigDecimal importPrice;
	private Integer lowStockThreshold;
	private String imageUrl;
}