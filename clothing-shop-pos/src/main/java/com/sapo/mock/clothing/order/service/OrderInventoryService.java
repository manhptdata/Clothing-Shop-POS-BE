package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.OrderLineItem;
import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.entity.StockLog;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.notification.service.NotificationService;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.receipt.repository.StockLogRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.constant.StockLogReferenceType;
import com.sapo.mock.clothing.util.constant.StockLogSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderInventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;
    private final StockLogRepository stockLogRepository;
    private final UserRepository userRepository;

    /**
     * Trừ tồn kho khi bán hàng và ghi StockLog audit trail.
     *
     * @param items     danh sách sản phẩm trong đơn
     * @param orderId   ID của đơn hàng (để ghi vào referenceId)
     * @param orderNumber mã đơn hàng (để ghi vào note)
     */
    public void deductProductStock(List<ReqCreateOrderDTO.OrderItemDTO> items,
                                   Integer orderId, String orderNumber) {
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : items) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm ID " + itemDto.getVariantId()));

            // Bug #7 fix: kiểm tra variant đã bị vô hiệu hóa
            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                throw new BadRequestException("Sản phẩm variant '" + variant.getSku()
                        + "' đã bị vô hiệu hóa, không thể bán.");
            }

            // Bug #6 fix: kiểm tra quantity hợp lệ
            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new BadRequestException("Số lượng sản phẩm phải lớn hơn 0");
            }

            if (variant.getQuantity() < itemDto.getQuantity()) {
                throw new BadRequestException("Sản phẩm ID " + itemDto.getVariantId()
                        + " không đủ số lượng (Hiện có: " + variant.getQuantity() + ")");
            }

            int qtyBefore = variant.getQuantity();
            int qtyAfter = qtyBefore - itemDto.getQuantity();
            variant.setQuantity(qtyAfter);
            ProductVariant savedVariant = productVariantRepository.save(variant);

            // Bug #19 fix: Ghi StockLog cho hành động bán hàng
            StockLog log = new StockLog();
            log.setVariant(savedVariant);
            log.setQuantityBefore(qtyBefore);
            log.setQuantityChange(-itemDto.getQuantity());
            log.setQuantityAfter(qtyAfter);
            log.setSource(StockLogSource.BAN_HANG);
            log.setReferenceType(StockLogReferenceType.INVOICE);
            log.setReferenceId(orderId);
            log.setNote("Bán hàng đơn " + orderNumber);

            Integer userId = SecurityUtil.getCurrentUserId();
            if (userId != null) {
                log.setCreatedBy(userRepository.getReferenceById(userId));
            }

            stockLogRepository.save(log);

            // Cảnh báo tồn kho thấp
            if (savedVariant.getQuantity() <= savedVariant.getLowStockThreshold()) {
                try {
                    Notification lowStockAlert = new Notification();
                    lowStockAlert.setTitle("Cảnh báo tồn kho chạm ngưỡng");

                    String fullName = savedVariant.getProduct().getName();
                    List<String> options = new ArrayList<>();
                    if (savedVariant.getOption1Value() != null)
                        options.add(savedVariant.getOption1Value().getValue());
                    if (savedVariant.getOption2Value() != null)
                        options.add(savedVariant.getOption2Value().getValue());
                    if (savedVariant.getOption3Value() != null)
                        options.add(savedVariant.getOption3Value().getValue());
                    if (!options.isEmpty())
                        fullName += " (" + String.join(" - ", options) + ")";

                    lowStockAlert.setMessage(
                            String.format("Cảnh báo: Mặt hàng [%s] %s đã chạm ngưỡng tồn kho tối thiểu (Còn %d chiếc).",
                                    savedVariant.getSku(), fullName, savedVariant.getQuantity()));
                    lowStockAlert.setType("LOW_STOCK");
                    lowStockAlert.setTargetRole("ROLE_WH");
                    lowStockAlert.setMetadata(String.format("{\"variantId\":%d,\"sku\":\"%s\",\"quantity\":%d}",
                            savedVariant.getId(), savedVariant.getSku(), savedVariant.getQuantity()));
                    notificationService.sendNotification(lowStockAlert);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi thông báo tồn kho: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Hoàn kho khi hủy đơn hàng và ghi StockLog audit trail.
     *
     * @param items       danh sách sản phẩm cần hoàn kho
     * @param orderId     ID đơn hàng gốc
     * @param orderNumber mã đơn hàng gốc
     */
    public void restoreProductStock(List<OrderLineItem> items, Integer orderId, String orderNumber) {
        for (OrderLineItem item : items) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy thông tin tồn kho của sản phẩm ID " + item.getVariantId()));

            int qtyBefore = variant.getQuantity();
            int qtyAfter = qtyBefore + item.getQuantity();
            variant.setQuantity(qtyAfter);
            ProductVariant savedVariant = productVariantRepository.save(variant);

            // Bug #19 fix: Ghi StockLog cho hành động hủy đơn → hoàn kho
            StockLog log = new StockLog();
            log.setVariant(savedVariant);
            log.setQuantityBefore(qtyBefore);
            log.setQuantityChange(item.getQuantity());
            log.setQuantityAfter(qtyAfter);
            log.setSource(StockLogSource.HUY_DON);
            log.setReferenceType(StockLogReferenceType.INVOICE);
            log.setReferenceId(orderId);
            log.setNote("Hoàn kho do hủy đơn " + orderNumber);

            Integer userId = SecurityUtil.getCurrentUserId();
            if (userId != null) {
                log.setCreatedBy(userRepository.getReferenceById(userId));
            }

            stockLogRepository.save(log);
        }
    }
}
