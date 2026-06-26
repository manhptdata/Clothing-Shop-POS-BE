package com.sapo.mock.clothing.receipt.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapo.mock.clothing.entity.ProductVariant; // Thêm import này
import com.sapo.mock.clothing.entity.StockLog;
import com.sapo.mock.clothing.entity.StockReceipt;
import com.sapo.mock.clothing.entity.StockReceiptItem;
import com.sapo.mock.clothing.entity.Supplier;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository; // Thêm import này
import com.sapo.mock.clothing.receipt.DTO.StockReceiptRequest;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptResponse;
import com.sapo.mock.clothing.receipt.repository.StockLogRepository;
import com.sapo.mock.clothing.receipt.repository.StockReceiptRepository;
import com.sapo.mock.clothing.supplier.repository.SupplierRepository;
import com.sapo.mock.clothing.util.constant.ReceiptStatus;
import com.sapo.mock.clothing.util.constant.StockLogReferenceType;
import com.sapo.mock.clothing.util.constant.StockLogSource;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockReceiptService implements IStockReceiptService {

	private final StockReceiptRepository receiptRepository;
	private final StockLogRepository stockLogRepository;
	private final SupplierRepository supplierRepository;

	// Thay thế WarehouseStockRepository bằng ProductVariantRepository
	private final ProductVariantRepository variantRepository;

	@Override
	@Transactional
	public StockReceiptResponse createReceipt(StockReceiptRequest request, Integer userId) {

		Supplier supplier = supplierRepository.findByIdAndActiveTrue(request.getSupplierId())
				.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy nhà cung cấp hoặc đã ngừng hợp tác"));

		StockReceipt receipt = new StockReceipt();

		// Sinh mã phiếu tự động dựa trên timestamp
		receipt.setCode("PN-" + System.currentTimeMillis());

		receipt.setSupplier(supplier);
		receipt.setNote(request.getNote());
		receipt.setStatus(ReceiptStatus.DRAFT);
//		receipt.setCreatedBy(userId);

		int totalQty = 0;
		BigDecimal totalAmt = BigDecimal.ZERO;
		;
		List<StockReceiptItem> items = new ArrayList<>();

		for (StockReceiptRequest.StockReceiptItemRequest itemReq : request.getItems()) {
			if (itemReq.getQuantity() <= 0) {
				throw new BadRequestException("Số lượng nhập phải lớn hơn 0");
			}

			ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
					.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy variant sản phẩm"));

			StockReceiptItem item = new StockReceiptItem();
			item.setReceipt(receipt);
			item.setVariant(variant);
			item.setQuantity(itemReq.getQuantity());
			item.setImportPrice(itemReq.getImportPrice());

			totalQty += itemReq.getQuantity();

			BigDecimal price = (itemReq.getImportPrice() != null) ? itemReq.getImportPrice() : BigDecimal.ZERO;

			totalAmt = totalAmt.add(price.multiply(BigDecimal.valueOf(itemReq.getQuantity())));

			items.add(item);
		}

		receipt.setItems(items);
		receipt.setTotalQuantity(totalQty);

		// Lưu xuống DB
		StockReceipt savedReceipt = receiptRepository.save(receipt);

		return mapToResponse(savedReceipt);
	}

	@Override
	@Transactional
	public StockReceiptResponse updateReceipt(Integer receiptId, StockReceiptRequest request, Integer userId) {
		StockReceipt receipt = receiptRepository.findById(receiptId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));

		if (receipt.getStatus() != ReceiptStatus.DRAFT) {
			throw new BadRequestException("Chỉ có thể sửa phiếu nhập khi đang ở trạng thái nháp");
		}

		Supplier supplier = supplierRepository.findByIdAndActiveTrue(request.getSupplierId())
				.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy nhà cung cấp hoặc đã ngừng hợp tác"));

		receipt.setSupplier(supplier);
		receipt.setNote(request.getNote());

		int totalQty = 0;
		BigDecimal totalAmt = BigDecimal.ZERO;

		// Xóa toàn bộ item cũ
		receipt.getItems().clear();

		for (StockReceiptRequest.StockReceiptItemRequest itemReq : request.getItems()) {
			if (itemReq.getQuantity() <= 0) {
				throw new BadRequestException("Số lượng nhập phải lớn hơn 0");
			}

			ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
					.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy variant sản phẩm"));

			StockReceiptItem item = new StockReceiptItem();
			item.setReceipt(receipt);
			item.setVariant(variant);
			item.setQuantity(itemReq.getQuantity());
			item.setImportPrice(itemReq.getImportPrice());

			totalQty += itemReq.getQuantity();

			BigDecimal price = (itemReq.getImportPrice() != null) ? itemReq.getImportPrice() : BigDecimal.ZERO;
			totalAmt = totalAmt.add(price.multiply(BigDecimal.valueOf(itemReq.getQuantity())));

			receipt.getItems().add(item);
		}

		receipt.setTotalQuantity(totalQty);

		StockReceipt savedReceipt = receiptRepository.save(receipt);
		return mapToResponse(savedReceipt);
	}

	@Override
	@Transactional
	public StockReceiptResponse confirmReceipt(Integer receiptId, Integer userId) {
		StockReceipt receipt = receiptRepository.findById(receiptId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));

		// SỬA LỖI ENUM: Dùng ReceiptStatus thay vì String
		if (receipt.getStatus() != ReceiptStatus.DRAFT) {
			throw new BadRequestException("Phiếu nhập đã được duyệt hoặc bị hủy, không thể thao tác lại");
		}

		// 1. Chuyển trạng thái bằng Enum
		receipt.setStatus(ReceiptStatus.CONFIRMED);
		receipt.setConfirmedAt(Instant.now());
//		receipt.setConfirmedBy(userId);

		// 2. Vòng lặp cộng tồn kho thẳng vào ProductVariant & Ghi Log
		for (StockReceiptItem item : receipt.getItems()) {
			// Tìm biến thể thay vì tìm WarehouseStock
			ProductVariant variant = variantRepository.findById(item.getVariant().getId()).orElseThrow(
					() -> new BadRequestException("Không tìm thấy biến thể SP ID: " + item.getVariant().getId()));

			// Xử lý an toàn nếu quantity đang null
			int oldQuantity = variant.getQuantity() != null ? variant.getQuantity() : 0;
			int newQuantity = oldQuantity + item.getQuantity();

			// Cập nhật tồn kho vật lý
			variant.setQuantity(newQuantity);
			variantRepository.save(variant);

			// 3. Ghi Audit Trail
			StockLog log = new StockLog();
			// Xóa dòng log.setWarehouseId()
			log.setVariant(variant);
			log.setQuantityBefore(oldQuantity);
			log.setQuantityChange(item.getQuantity());
			log.setQuantityAfter(newQuantity);
			log.setReferenceId(receipt.getId());
			log.setReferenceType(StockLogReferenceType.RECEIPT);
			log.setSource(StockLogSource.NHAP_HANG);
			log.setNote("Nhập hàng từ phiếu " + receipt.getCode());

			stockLogRepository.save(log);
		}

		receiptRepository.save(receipt);
		return mapToResponse(receipt);
	}

	@Override
	public StockReceiptResponse getReceiptById(Integer receiptId) {
		StockReceipt receipt = receiptRepository.findById(receiptId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));
		return mapToResponse(receipt);
	}

	private StockReceiptResponse mapToResponse(StockReceipt entity) {
		if (entity == null) {
			return null;
		}

		StockReceiptResponse response = new StockReceiptResponse();
		response.setId(entity.getId());
		response.setCode(entity.getCode());

		// Map trạng thái Enum sang String
		if (entity.getStatus() != null) {
			response.setStatus(entity.getStatus().name());
		}

		// Map ID nhà cung cấp
		if (entity.getSupplier() != null) {
			response.setSupplierId(entity.getSupplier().getId());
		}

		response.setNote(entity.getNote());
		response.setTotalQuantity(entity.getTotalQuantity());
		response.setCreatedAt(entity.getCreatedAt());
		response.setCreatedBy(entity.getCreatedBy());
		response.setConfirmedAt(entity.getConfirmedAt());
		response.setConfirmedBy(entity.getConfirmedBy());

		// Map danh sách sản phẩm con và Tính tổng tiền
		if (entity.getItems() != null) {
			BigDecimal totalAmount = BigDecimal.ZERO;
			List<StockReceiptResponse.StockReceiptItemResponse> itemResponses = new ArrayList<>();

			for (StockReceiptItem item : entity.getItems()) {
				StockReceiptResponse.StockReceiptItemResponse itemRes = new StockReceiptResponse.StockReceiptItemResponse();
				itemRes.setId(item.getId());
				itemRes.setQuantity(item.getQuantity());
				itemRes.setImportPrice(item.getImportPrice());

				if (item.getVariant() != null) {
					itemRes.setVariantId(item.getVariant().getId());
					itemRes.setSku(item.getVariant().getSku());
				}

				// Tính tổng tiền phiếu = Giá nhập * Số lượng
				if (item.getImportPrice() != null && item.getQuantity() > 0) {
					totalAmount = totalAmount
							.add(item.getImportPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
				}

				itemResponses.add(itemRes);
			}

			response.setItems(itemResponses);
			response.setTotalAmount(totalAmount);
		} else {
			response.setTotalAmount(BigDecimal.ZERO);
		}

		return response;
	}

	@Override
	public Page<StockReceiptResponse> getAllReceipts(Pageable pageable) {

		return receiptRepository.findAll(pageable).map(this::mapToResponse);
	}
}