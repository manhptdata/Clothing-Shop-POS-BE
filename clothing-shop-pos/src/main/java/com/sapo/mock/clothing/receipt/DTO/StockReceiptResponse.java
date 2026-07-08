package com.sapo.mock.clothing.receipt.DTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class StockReceiptResponse {
	private Integer id;
	private String code; // Mã phiếu nhập (VD: PN-123456)

	// Đã xóa bỏ warehouseId

	private Integer supplierId;
	private String supplierName;
	private String status; // DRAFT hoặc CONFIRMED
	private String note;
	private Integer totalQuantity;

	// Chuyển sang BigDecimal
	private BigDecimal totalAmount;

	// Chuyển sang Instant để đồng bộ với Entity
	private Instant createdAt;
	private Integer createdBy;
	private Instant confirmedAt;
	private Integer confirmedBy;

	private List<StockReceiptItemResponse> items;

	@Data
	public static class StockReceiptItemResponse {
		private Integer id;
		private Integer variantId;
		private String sku;
		private Integer quantity;

		// Chuyển sang BigDecimal
		private BigDecimal importPrice;
	}
}