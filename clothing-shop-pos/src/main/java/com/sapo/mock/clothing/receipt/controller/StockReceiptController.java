package com.sapo.mock.clothing.receipt.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptRequest;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptResponse;
import com.sapo.mock.clothing.receipt.service.IStockReceiptService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class StockReceiptController {

	private final IStockReceiptService receiptService;

	// Mock userId để test tạm thời khi chưa tích hợp Security (JWT Token)
	private final Integer MOCK_USER_ID = 1;

	// 1. Tạo phiếu nhập kho (Bản nháp)
	@PostMapping
	public ResponseEntity<RestResponse<StockReceiptResponse>> createReceipt(
			@Valid @RequestBody StockReceiptRequest request) {
		StockReceiptResponse receipt = receiptService.createReceipt(request, MOCK_USER_ID);
		RestResponse<StockReceiptResponse> response = new RestResponse<>(201, null,
				"Tạo phiếu nhập kho (Nháp) thành công", receipt);
		return ResponseEntity.status(201).body(response);
	}

	// 2. Duyệt phiếu nhập (Cộng tồn kho)
	@PostMapping("/{id}/confirm")
	public ResponseEntity<RestResponse<StockReceiptResponse>> confirmReceipt(@PathVariable Integer id) {
		StockReceiptResponse receipt = receiptService.confirmReceipt(id, MOCK_USER_ID);
		RestResponse<StockReceiptResponse> response = new RestResponse<>(200, null,
				"Duyệt phiếu và cộng tồn kho thành công", receipt);
		return ResponseEntity.ok(response);
	}

	// 3. Xem chi tiết phiếu nhập
	@GetMapping("/{id}")
	public ResponseEntity<RestResponse<StockReceiptResponse>> getReceiptById(@PathVariable Integer id) {
		StockReceiptResponse receipt = receiptService.getReceiptById(id);
		RestResponse<StockReceiptResponse> response = new RestResponse<>(200, null,
				"Lấy thông tin phiếu nhập thành công", receipt);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<RestResponse<Page<StockReceiptResponse>>> getAllReceipts(Pageable pageable) {
		Page<StockReceiptResponse> receipts = receiptService.getAllReceipts(pageable);
		RestResponse<Page<StockReceiptResponse>> response = new RestResponse<>(200, null,
				"Lấy danh sách phiếu nhập thành công", receipts);
		return ResponseEntity.ok(response);
	}
}