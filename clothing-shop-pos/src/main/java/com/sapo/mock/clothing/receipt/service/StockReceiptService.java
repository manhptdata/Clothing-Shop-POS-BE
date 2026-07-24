package com.sapo.mock.clothing.receipt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import com.sapo.mock.clothing.notification.service.NotificationService;
import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.util.constant.NotificationConstants;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptRequest;
import com.sapo.mock.clothing.receipt.DTO.StockReceiptResponse;
import com.sapo.mock.clothing.receipt.repository.StockLogRepository;
import com.sapo.mock.clothing.receipt.repository.StockReceiptRepository;
import com.sapo.mock.clothing.specification.StockReceiptSpecification;
import com.sapo.mock.clothing.supplier.repository.SupplierRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
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
	private final ProductVariantRepository variantRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	@Override
	@Transactional
	public StockReceiptResponse createReceipt(StockReceiptRequest request, Integer userId) {

		Supplier supplier = supplierRepository.findByIdAndActiveTrue(request.getSupplierId())
				.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy nhà cung cấp hoặc đã ngừng hợp tác"));

		StockReceipt receipt = new StockReceipt();

		// Sinh mã phiếu tự động dựa trên timestamp và chuỗi ngẫu nhiên
		int randomNum = (int) (Math.random() * 1000);
		receipt.setCode("PN-" + System.currentTimeMillis() + "-" + String.format("%03d", randomNum));

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

			if (!variant.getIsActive()) {
				throw new BadRequestException("Biến thể '" + variant.getSku() + "' đã bị vô hiệu hóa, không thể nhập kho.");
			}

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
		receipt.setTotalAmount(totalAmt); // Bug #11 fix: lưu tổng tiền vào DB
		receipt.setCreatedBy(userId);     // Bug #12 fix: bỏ comment để lưu người tạo
		StockReceipt savedReceipt = receiptRepository.save(receipt);

		Notification notification = new Notification();
		notification.setTitle("Tạo phiếu nhập kho");
		notification.setMessage("Phiếu nhập kho '" + savedReceipt.getCode() + "' vừa được tạo mới.");
		notification.setType("SYSTEM");
		notification.setTargetRole(NotificationConstants.TARGET_MANAGEMENT);
		notification.setMetadata("{\"receiptId\":" + savedReceipt.getId() + "}");
		notificationService.sendNotification(notification);

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

			if (!variant.getIsActive()) {
				throw new BadRequestException("Biến thể '" + variant.getSku() + "' đã bị vô hiệu hóa, không thể nhập kho.");
			}

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
		receipt.setTotalAmount(totalAmt); // Bug #11 fix: lưu tổng tiền khi cập nhật

		StockReceipt savedReceipt = receiptRepository.save(receipt);

		Notification notification = new Notification();
		notification.setTitle("Cập nhật phiếu nhập kho");
		notification.setMessage("Phiếu nhập kho '" + savedReceipt.getCode() + "' vừa được cập nhật.");
		notification.setType("SYSTEM");
		notification.setTargetRole(NotificationConstants.TARGET_MANAGEMENT);
		notification.setMetadata("{\"receiptId\":" + savedReceipt.getId() + "}");
		notificationService.sendNotification(notification);

		return mapToResponse(savedReceipt);
	}

	@Override
	@Transactional
	public StockReceiptResponse confirmReceipt(Integer receiptId, Integer userId) {
		StockReceipt receipt = receiptRepository.findByIdWithPessimisticLock(receiptId)
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
		// Anti-deadlock: Sắp xếp items theo variant ID tăng dần
		List<StockReceiptItem> sortedReceiptItems = new ArrayList<>(receipt.getItems());
		sortedReceiptItems.sort(java.util.Comparator.comparing(i -> i.getVariant().getId()));

		for (StockReceiptItem item : sortedReceiptItems) {
			// Tìm biến thể thay vì tìm WarehouseStock
			ProductVariant variant = variantRepository.findByIdWithPessimisticLock(item.getVariant().getId()).orElseThrow(
					() -> new BadRequestException("Không tìm thấy biến thể SP ID: " + item.getVariant().getId()));

			if (!variant.getIsActive()) {
				throw new BadRequestException("Biến thể '" + variant.getSku() + "' đã bị vô hiệu hóa, không thể nhập kho.");
			}

			// Xử lý an toàn nếu quantity đang null
			int oldQuantity = variant.getQuantity() != null ? variant.getQuantity() : 0;
			int newQuantity = oldQuantity + item.getQuantity();

			// Tính giá bình quân gia quyền di động (MAC)
			BigDecimal oldImportPrice = variant.getImportPrice() != null ? variant.getImportPrice() : BigDecimal.ZERO;
			BigDecimal itemImportPrice = item.getImportPrice() != null ? item.getImportPrice() : BigDecimal.ZERO;
			BigDecimal newImportPrice;

			if (oldQuantity <= 0) {
				newImportPrice = itemImportPrice;
			} else {
				BigDecimal totalOldValue = oldImportPrice.multiply(BigDecimal.valueOf(oldQuantity));
				BigDecimal totalNewValue = itemImportPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
				BigDecimal totalQuantity = BigDecimal.valueOf(newQuantity);
				if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
					newImportPrice = totalOldValue.add(totalNewValue).divide(totalQuantity, 2, RoundingMode.HALF_UP);
				} else {
					newImportPrice = BigDecimal.ZERO;
				}
			}

			// Cập nhật tồn kho vật lý và giá vốn bình quân
			variant.setQuantity(newQuantity);
			variant.setImportPrice(newImportPrice);
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

			if (userId != null) {
				log.setCreatedBy(userRepository.getReferenceById(userId));
			}

			stockLogRepository.save(log);
		}

		receiptRepository.save(receipt);

		Notification notification = new Notification();
		notification.setTitle("Duyệt phiếu nhập kho");
		notification.setMessage("Phiếu nhập kho '" + receipt.getCode() + "' vừa được duyệt thành công.");
		notification.setType("SYSTEM");
		notification.setTargetRole(NotificationConstants.TARGET_MANAGEMENT);
		notificationService.sendNotification(notification);

		return mapToResponse(receipt);
	}

	@Override
	@Transactional
	public StockReceiptResponse cancelReceipt(Integer receiptId, Integer userId) {
		StockReceipt receipt = receiptRepository.findByIdWithPessimisticLock(receiptId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));

		if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
			throw new BadRequestException("Phiếu nhập đã bị hủy trước đó");
		}

		if (receipt.getStatus() == ReceiptStatus.CONFIRMED) {
			throw new BadRequestException("Không thể hủy Phiếu Nhập đã được Duyệt. Để đảm bảo tính toàn vẹn của Giá Vốn (MAC) và Kế toán, vui lòng sử dụng chức năng Trả Hàng Cho Nhà Cung Cấp hoặc Điều Chỉnh Kho (Kiểm kê).");
		}

		receipt.setStatus(ReceiptStatus.CANCELLED);
		receiptRepository.save(receipt);

		Notification notification = new Notification();
		notification.setTitle("Hủy phiếu nhập kho");
		notification.setMessage("Phiếu nhập kho '" + receipt.getCode() + "' đã bị hủy.");
		notification.setType("SYSTEM");
		notification.setTargetRole(NotificationConstants.TARGET_MANAGEMENT);
		notificationService.sendNotification(notification);

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

		// Map thông tin nhà cung cấp
		if (entity.getSupplier() != null) {
			response.setSupplierId(entity.getSupplier().getId());
			response.setSupplierName(entity.getSupplier().getName());
		}

		response.setNote(entity.getNote());
		response.setTotalQuantity(entity.getTotalQuantity());
		response.setCreatedAt(entity.getCreatedAt());
		response.setCreatedBy(entity.getCreatedBy());
		response.setConfirmedAt(entity.getConfirmedAt());
		response.setConfirmedBy(entity.getConfirmedBy());

		if (entity.getCreatedBy() != null) {
			userRepository.findById(entity.getCreatedBy())
					.ifPresent(user -> response.setCreatedByUsername(user.getFullName() != null ? user.getFullName() : user.getUsername()));
		}
		if (entity.getConfirmedBy() != null) {
			userRepository.findById(entity.getConfirmedBy())
					.ifPresent(user -> response.setConfirmedByUsername(user.getFullName() != null ? user.getFullName() : user.getUsername()));
		}

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
	public Page<StockReceiptResponse> getAllReceipts(String search, ReceiptStatus status, Pageable pageable) {
		Specification<StockReceipt> spec = StockReceiptSpecification.filterReceipts(search, status);
		Page<StockReceipt> pageResult = receiptRepository.findAll(spec, pageable);
		return pageResult.map(this::mapToResponse);
	}
}