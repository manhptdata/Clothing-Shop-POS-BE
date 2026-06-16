package com.sapo.mock.clothing.receipt.DTO;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockReceiptRequest {
//	@NotNull(message = "ID Kho không được để trống")
//	private Integer warehouseId;

	@NotNull(message = "ID Nhà cung cấp không được để trống")
	private Integer supplierId;

	private String note;

	@NotEmpty(message = "Danh sách sản phẩm nhập không được để trống")
	private List<StockReceiptItemRequest> items;

	@Data
	public static class StockReceiptItemRequest {
		@NotNull(message = "ID Biến thể không được để trống")
		private Integer variantId;

		@NotNull(message = "Số lượng nhập không được để trống")
		private Integer quantity;

		private BigDecimal importPrice;
	}
}