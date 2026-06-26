package com.sapo.mock.clothing.receipt.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sapo.mock.clothing.receipt.DTO.StockReceiptRequest;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptResponse;
import com.sapo.mock.clothing.util.constant.ReceiptStatus;

public interface IStockReceiptService {
	StockReceiptResponse createReceipt(StockReceiptRequest request, Integer userId);

	StockReceiptResponse updateReceipt(Integer receiptId, StockReceiptRequest request, Integer userId);

	StockReceiptResponse confirmReceipt(Integer receiptId, Integer userId);

	StockReceiptResponse cancelReceipt(Integer receiptId, Integer userId);

	StockReceiptResponse getReceiptById(Integer receiptId);

	Page<StockReceiptResponse> getAllReceipts(String search, ReceiptStatus status, Pageable pageable);

}