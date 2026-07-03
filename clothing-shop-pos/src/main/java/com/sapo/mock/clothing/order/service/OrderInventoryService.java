package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.OrderLineItem;
import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.notification.service.NotificationService;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderInventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;

    public void deductProductStock(List<ReqCreateOrderDTO.OrderItemDTO> items) {
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : items) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm ID " + itemDto.getVariantId()));

            if (variant.getQuantity() < itemDto.getQuantity()) {
                throw new BadRequestException("Sản phẩm ID " + itemDto.getVariantId() + " không đủ số lượng (Hiện có: "
                        + variant.getQuantity() + ")");
            }

            variant.setQuantity(variant.getQuantity() - itemDto.getQuantity());
            ProductVariant savedVariant = productVariantRepository.save(variant);

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

    public void restoreProductStock(List<OrderLineItem> items) {
        for (OrderLineItem item : items) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy thông tin tồn kho của sản phẩm ID " + item.getVariantId()));
            variant.setQuantity(variant.getQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }
    }
}
