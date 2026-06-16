package com.sapo.mock.clothing.receipt.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sapo.mock.clothing.receipt.DTO.StockReceiptRequest;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptResponse;

public interface IStockReceiptService {
	StockReceiptResponse createReceipt(StockReceiptRequest request, Integer userId);

	StockReceiptResponse confirmReceipt(Integer receiptId, Integer userId);

	StockReceiptResponse getReceiptById(Integer receiptId);

	Page<StockReceiptResponse> getAllReceipts(Pageable pageable);

}