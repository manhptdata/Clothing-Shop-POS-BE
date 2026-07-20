package com.sapo.mock.clothing.returnorder.service;

import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import com.sapo.mock.clothing.customer.repository.VoucherRepository;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.entity.OrderLineItem;
import com.sapo.mock.clothing.entity.PointHistory;
import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.entity.ReturnOrder;
import com.sapo.mock.clothing.entity.ReturnOrderLineItem;
import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.returnorder.dto.ReqCreateReturnDTO;
import com.sapo.mock.clothing.returnorder.dto.ResReturnOrderDTO;
import com.sapo.mock.clothing.returnorder.repository.ReturnOrderLineItemRepository;
import com.sapo.mock.clothing.returnorder.repository.ReturnOrderRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.util.constant.PointConstant;
import com.sapo.mock.clothing.receipt.repository.StockLogRepository;
import com.sapo.mock.clothing.util.constant.StockLogReferenceType;
import com.sapo.mock.clothing.util.constant.StockLogSource;
import com.sapo.mock.clothing.entity.StockLog;
import com.sapo.mock.clothing.setting.service.SystemSettingService;
import com.sapo.mock.clothing.order.service.OrderLoyaltyService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.sapo.mock.clothing.specification.ReturnOrderSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReturnOrderService {

    private final ReturnOrderRepository returnOrderRepository;
    private final ReturnOrderLineItemRepository returnOrderLineItemRepository;
    private final OrderRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherRepository voucherRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StockLogRepository stockLogRepository;
    private final SystemSettingService systemSettingService;
    private final PasswordEncoder passwordEncoder;
    private final OrderLoyaltyService orderLoyaltyService;

    @Transactional
    public ResReturnOrderDTO createReturn(ReqCreateReturnDTO dto, String username) {
        User createdBy = userRepository.findByUsername(username);
        if (createdBy == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        }

        String approvedByUsername = createdBy.getUsername();

        // 0. Verify PIN if required
        if (systemSettingService.isReturnApprovalRequired()) {
            if (dto.getApprovalPin() == null || dto.getApprovalPin().isEmpty()) {
                throw new BadRequestException("Cần có mã PIN quản lý để duyệt phiếu trả hàng.");
            }

            // Find an admin/manager who matches this PIN
            List<User> approvers = userRepository.findAll().stream()
                    .filter(u -> "ROLE_ADMIN".equals(u.getRole().getName()) || "ROLE_MANAGER".equals(u.getRole().getName()))
                    .filter(u -> u.getSecurityPin() != null && u.getSecurityPin().equals(dto.getApprovalPin()))
                    .toList();

            if (approvers.isEmpty()) {
                throw new BadRequestException("Mã PIN không chính xác hoặc người duyệt không có quyền.");
            }
            approvedByUsername = approvers.get(0).getUsername();
        }


        Order order = orderRepository.findByIdWithPessimisticLock(dto.getOriginalOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn gốc ID " + dto.getOriginalOrderId() + " không tồn tại"));

        // 1. Kiểm tra trạng thái đơn hàng gốc
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.PARTIALLY_RETURNED) {
            throw new BadRequestException("Chỉ được trả hàng cho hóa đơn có trạng thái Hoàn thành hoặc Trả hàng một phần");
        }

        // 2. Kiểm tra giới hạn thời gian (7 ngày)
        Instant limitTime = Instant.now().minus(7, ChronoUnit.DAYS);
        if (order.getCreatedAt().isBefore(limitTime)) {
            throw new BadRequestException("Hóa đơn đã mua quá 7 ngày, không hỗ trợ trả hàng.");
        }

        // Load customer thông thường để lấy thông tin (tên, id) cho phiếu trả hàng.
        // Sẽ tải lại có Lock ngay trước khi thao tác điểm thưởng ở bên dưới.
        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại"));

        List<OrderLineItem> originalItems = orderLineItemRepository.findByOrderId(order.getId());
        Map<Integer, OrderLineItem> originalItemsMap = new HashMap<>();
        for (OrderLineItem item : originalItems) {
            originalItemsMap.put(item.getVariantId(), item);
        }

        List<ReturnOrder> existingReturns = returnOrderRepository.findByOrderId(order.getId());
        Map<Integer, Integer> alreadyReturnedMap = new HashMap<>();
        for (ReturnOrder ro : existingReturns) {
            for (ReturnOrderLineItem item : ro.getItems()) {
                alreadyReturnedMap.put(item.getVariantId(), alreadyReturnedMap.getOrDefault(item.getVariantId(), 0) + item.getQuantity());
            }
        }

        // 3. Tính toán chiết khấu tỷ lệ của đơn hàng gốc
        BigDecimal totalDiscount = (order.getDiscountFromVoucher() != null ? order.getDiscountFromVoucher() : BigDecimal.ZERO)
                .add(order.getDiscountFromPoints() != null ? order.getDiscountFromPoints() : BigDecimal.ZERO);
        
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        for (OrderLineItem item : originalItems) {
            originalSubtotal = originalSubtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountRatio = BigDecimal.ZERO;
        if (originalSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            discountRatio = totalDiscount.divide(originalSubtotal, 6, RoundingMode.HALF_UP);
        }

        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setOrder(order);
        returnOrder.setCustomerId(customer.getId());
        returnOrder.setCustomerName(customer.getFullName());
        returnOrder.setCreatedBy(createdBy.getId());
        returnOrder.setCreatedByUsername(createdBy.getUsername());
        returnOrder.setApprovedByUsername(approvedByUsername);
        returnOrder.setReason(dto.getReason());

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        String returnNumber = "RTN-" + dateStr + "-" + timeStr;
        returnOrder.setReturnNumber(returnNumber);

        // Gom nhóm số lượng trả lại từ DTO
        Map<Integer, Integer> dtoQuantityMap = new HashMap<>();
        for (ReqCreateReturnDTO.ReturnItemDTO ri : dto.getItems()) {
            dtoQuantityMap.put(ri.getVariantId(), dtoQuantityMap.getOrDefault(ri.getVariantId(), 0) + ri.getQuantity());
        }

        boolean isAllReturned = true;
        for (OrderLineItem originalItem : originalItems) {
            int currentReturnedInDto = dtoQuantityMap.getOrDefault(originalItem.getVariantId(), 0);
            int totalReturnedForVariant = alreadyReturnedMap.getOrDefault(originalItem.getVariantId(), 0) + currentReturnedInDto;
            if (totalReturnedForVariant < originalItem.getQuantity()) {
                isAllReturned = false;
                break;
            }
        }

        List<ReturnOrderLineItem> returnLineItems = new ArrayList<>();
        BigDecimal computedRefundAmount = BigDecimal.ZERO;
        BigDecimal returnedItemsSubtotal = BigDecimal.ZERO;

        for (ReqCreateReturnDTO.ReturnItemDTO returnItemDto : dto.getItems()) {
            OrderLineItem originalItem = originalItemsMap.get(returnItemDto.getVariantId());
            if (originalItem == null) {
                throw new BadRequestException("Sản phẩm ID " + returnItemDto.getVariantId() + " không có trong hóa đơn gốc.");
            }

            int alreadyReturned = alreadyReturnedMap.getOrDefault(returnItemDto.getVariantId(), 0);
            int remainingAllowed = originalItem.getQuantity() - alreadyReturned;

            if (returnItemDto.getQuantity() > remainingAllowed) {
                throw new BadRequestException("Sản phẩm " + originalItem.getProductName() + " đã trả " + alreadyReturned 
                        + " chiếc. Số lượng muốn trả tiếp (" + returnItemDto.getQuantity() + ") vượt quá số lượng còn lại có thể trả (" + remainingAllowed + ").");
            }
            
            alreadyReturnedMap.put(returnItemDto.getVariantId(), alreadyReturned + returnItemDto.getQuantity());

            ProductVariant variant = productVariantRepository.findByIdWithPessimisticLock(returnItemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm phân loại ID " + returnItemDto.getVariantId() + " không tồn tại"));

            // Hoàn trả tồn kho
            int qtyBefore = variant.getQuantity();
            int qtyAfter = qtyBefore + returnItemDto.getQuantity();
            variant.setQuantity(qtyAfter);
            productVariantRepository.save(variant);

            // Bug #20 fix: Ghi StockLog cho hành động trả hàng
            StockLog stockLog = new StockLog();
            stockLog.setVariant(variant);
            stockLog.setQuantityBefore(qtyBefore);
            stockLog.setQuantityChange(returnItemDto.getQuantity());
            stockLog.setQuantityAfter(qtyAfter);
            stockLog.setSource(StockLogSource.TRA_HANG);
            stockLog.setReferenceType(StockLogReferenceType.RETURN);
            stockLog.setReferenceId(order.getId());
            stockLog.setNote("Khách trả hàng đơn " + order.getOrderNumber());
            stockLog.setCreatedBy(createdBy);
            stockLogRepository.save(stockLog);

            // Tính tổng tiền hoàn lại của món này (làm tròn ở bước cuối cùng để tránh sai số)
            BigDecimal unitPrice = originalItem.getUnitPrice();
            BigDecimal refundPriceUnrounded = unitPrice.multiply(BigDecimal.ONE.subtract(discountRatio));
            BigDecimal itemSubtotal = refundPriceUnrounded.multiply(BigDecimal.valueOf(returnItemDto.getQuantity())).setScale(0, RoundingMode.HALF_UP);

            // Tính lại đơn giá hoàn trả để lưu database/hiển thị hóa đơn (bằng tổng / số lượng)
            BigDecimal refundPrice = itemSubtotal.divide(BigDecimal.valueOf(returnItemDto.getQuantity()), 0, RoundingMode.HALF_UP);

            ReturnOrderLineItem returnLineItem = new ReturnOrderLineItem();
            returnLineItem.setReturnOrder(returnOrder);
            returnLineItem.setVariantId(variant.getId());
            returnLineItem.setProductName(originalItem.getProductName());
            returnLineItem.setProductSku(originalItem.getProductSku());
            returnLineItem.setQuantity(returnItemDto.getQuantity());
            returnLineItem.setRefundPrice(refundPrice);
            returnLineItem.setSubtotal(itemSubtotal);

            returnLineItems.add(returnLineItem);
            computedRefundAmount = computedRefundAmount.add(itemSubtotal);
            returnedItemsSubtotal = returnedItemsSubtotal.add(originalItem.getUnitPrice().multiply(BigDecimal.valueOf(returnItemDto.getQuantity())));
        }

        BigDecimal unpenalizedCurrentRefund = computedRefundAmount;

        // Cửa hàng chỉ hoàn lại số tiền thực tế khách đã trả
        BigDecimal previousRefundTotal = returnOrderRepository.getTotalRefundedByOrderId(order.getId());
        if (previousRefundTotal == null) {
            previousRefundTotal = BigDecimal.ZERO;
        }

        BigDecimal remainingOrderPaidAmount = order.getTotalAmount().subtract(previousRefundTotal);
        if (computedRefundAmount.compareTo(remainingOrderPaidAmount) > 0) {
            computedRefundAmount = remainingOrderPaidAmount;
        }
        
        BigDecimal previousReturnedSubtotal = BigDecimal.ZERO;
        BigDecimal previousUnpenalizedRefundTotal = BigDecimal.ZERO;
        for (ReturnOrder ro : existingReturns) {
            for (ReturnOrderLineItem item : ro.getItems()) {
                OrderLineItem origItem = originalItemsMap.get(item.getVariantId());
                if (origItem != null) {
                    previousReturnedSubtotal = previousReturnedSubtotal.add(origItem.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
                previousUnpenalizedRefundTotal = previousUnpenalizedRefundTotal.add(item.getSubtotal());
            }
        }

        // Kiểm tra chặn nếu TRẢ 1 PHẦN làm rớt hóa đơn xuống dưới điều kiện Voucher
        // Fix Issue 2: Giá trị đơn hàng mới tính dựa trên giá trị hàng hóa đã trả (chưa trừ phạt) thay vì tiền đã hoàn (đã trừ phạt)
        BigDecimal newOrderValue = order.getTotalAmount().subtract(unpenalizedCurrentRefund).subtract(previousUnpenalizedRefundTotal);
        if (newOrderValue.compareTo(BigDecimal.ZERO) < 0) {
            newOrderValue = BigDecimal.ZERO;
        }

        BigDecimal newSubtotal = originalSubtotal.subtract(previousReturnedSubtotal).subtract(returnedItemsSubtotal);

        if (!isAllReturned && order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
            BigDecimal minOrderValue = null;
            com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher = customerVoucherRepository.findByOrderId(order.getId()).orElse(null);
            if (appliedVoucher != null && appliedVoucher.getVoucher() != null) {
                minOrderValue = appliedVoucher.getVoucher().getMinOrderValue();
            } else {
                com.sapo.mock.clothing.entity.Voucher publicVoucher = voucherRepository.findByCode(order.getVoucherCode()).orElse(null);
                if (publicVoucher != null) {
                    minOrderValue = publicVoucher.getMinOrderValue();
                }
            }
            if (minOrderValue != null && newSubtotal.compareTo(minOrderValue) < 0) {
                throw new BadRequestException("Việc trả một phần hàng làm đơn hàng không còn đủ điều kiện áp dụng Voucher (Min Order " + minOrderValue + "). Vui lòng yêu cầu khách trả toàn bộ đơn hàng (hoặc giữ nguyên đơn).");
            }
        }

        // Khấu trừ điểm và doanh số chi tiêu của khách hàng (Nếu không phải Khách vãng lai)
        if (customer.getId() != 1) {
            // Re-fetch với Pessimistic Lock trước khi thạo tác điểm thưởng để tránh Lost Update
            Customer lockedCustomer = customerRepository.findByIdWithPessimisticLock(customer.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customer.getId()));

            // 1. Hoàn lại điểm tiêu dùng theo tỷ lệ phần giá trị trả lại (Proportional Refund)
            if (order.getPointsUsed() > 0) {
                BigDecimal returnRatio = BigDecimal.ZERO;
                if (originalSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                    returnRatio = returnedItemsSubtotal.divide(originalSubtotal, 4, RoundingMode.HALF_UP);
                }

                int pointsToRefund = BigDecimal.valueOf(order.getPointsUsed()).multiply(returnRatio).intValue();

                if (pointsToRefund > 0) {
                    lockedCustomer.setRewardPoints(lockedCustomer.getRewardPoints() + pointsToRefund);
                    PointHistory phRefund = new PointHistory();
                    phRefund.setCustomerId(lockedCustomer.getId());
                    phRefund.setOrderId(order.getId());
                    phRefund.setPointsChange(pointsToRefund);
                    phRefund.setType(PointConstant.TYPE_REFUND);
                    phRefund.setDescription("Hoàn điểm tiêu dùng theo tỷ lệ do trả hàng đơn " + order.getOrderNumber());
                    pointHistoryRepository.save(phRefund);
                }
            }

            // 2. Khấu trừ điểm bằng công thức Chống Sai Số (Trả nhiều lần) - Fix Issue 3 (Double-Penalize)
            // Tính số điểm ĐÁNG LẼ phải trừ của các lần trước (dựa trên giá trị hàng hóa trả lại chưa trừ phạt)
            BigDecimal previousNewOrderValue = order.getTotalAmount().subtract(previousUnpenalizedRefundTotal);
            if (previousNewOrderValue.compareTo(BigDecimal.ZERO) < 0) previousNewOrderValue = BigDecimal.ZERO;
            int previousEarnedPoints = previousNewOrderValue.divideToIntegralValue(PointConstant.EARN_RATE).intValue();
            int previousTheoreticalDeducted = order.getPointsEarned() - previousEarnedPoints;
            if (previousTheoreticalDeducted < 0) previousTheoreticalDeducted = 0;

            // Tính TỔNG số điểm phải trừ tính đến hiện tại
            int currentEarnedPoints = newOrderValue.divideToIntegralValue(PointConstant.EARN_RATE).intValue();
            int currentTheoreticalDeductedTotal = order.getPointsEarned() - currentEarnedPoints;
            if (currentTheoreticalDeductedTotal < 0) currentTheoreticalDeductedTotal = 0;

            // Số điểm thực sự cần trừ CỦA LẦN NÀY (Bao gồm cả điểm bù bằng tiền mặt hoặc điểm ví)
            int pointsToDeduct = currentTheoreticalDeductedTotal - previousTheoreticalDeducted;
            if (pointsToDeduct > 0) {
                int currentPoints = lockedCustomer.getRewardPoints();
                if (currentPoints < pointsToDeduct) {
                    int deficitPoints = pointsToDeduct - currentPoints;
                    BigDecimal penaltyAmount = PointConstant.REDEEM_RATE.multiply(BigDecimal.valueOf(deficitPoints));
                    
                    computedRefundAmount = computedRefundAmount.subtract(penaltyAmount);
                    if (computedRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
                        computedRefundAmount = BigDecimal.ZERO;
                    }
                    
                    lockedCustomer.setRewardPoints(0);
                    
                    PointHistory phDeduct = new PointHistory();
                    phDeduct.setCustomerId(lockedCustomer.getId());
                    phDeduct.setOrderId(order.getId());
                    phDeduct.setPointsChange(-currentPoints);
                    phDeduct.setType(PointConstant.TYPE_REFUND);
                    phDeduct.setDescription("Khấu trừ toàn bộ điểm hiện có và cấn trừ " + penaltyAmount + " VNĐ vào tiền hoàn trả do khách trả hàng đơn " + order.getOrderNumber());
                    pointHistoryRepository.save(phDeduct);
                } else {
                    lockedCustomer.setRewardPoints(currentPoints - pointsToDeduct);
                    
                    PointHistory phDeduct = new PointHistory();
                    phDeduct.setCustomerId(lockedCustomer.getId());
                    phDeduct.setOrderId(order.getId());
                    phDeduct.setPointsChange(-pointsToDeduct);
                    phDeduct.setType(PointConstant.TYPE_REFUND);
                    phDeduct.setDescription("Khấu trừ điểm tích lũy do khách trả hàng đơn " + order.getOrderNumber());
                    pointHistoryRepository.save(phDeduct);
                }
            }
            customerRepository.save(lockedCustomer);

            // Gửi sự kiện cập nhật tổng chi tiêu (event với số tiền âm) để recheck nâng/hạ hạng tự động
            eventPublisher.publishEvent(new OrderCompletedEvent(lockedCustomer.getId(), computedRefundAmount.negate()));
        }

        returnOrder.setTotalRefundAmount(computedRefundAmount);

        // Lưu đơn trả hàng
        ReturnOrder savedReturnOrder = returnOrderRepository.save(returnOrder);
        returnOrderLineItemRepository.saveAll(returnLineItems);

        // Cập nhật trạng thái đơn hàng gốc

        if (isAllReturned) {
            order.setStatus(OrderStatus.RETURNED);
            
            // Chỉ phục hồi Voucher về trạng thái UNUSED (Không được gọi hàm hoàn/trừ điểm vì đã tính toán trên vòng lặp)
            orderLoyaltyService.revertVoucherOnlyOnCancel(order, customer);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RETURNED);
        }
        
        orderRepository.save(order);

        return mapToResReturnOrderDTO(savedReturnOrder, returnLineItems);
    }

    public ResReturnOrderDTO getReturnById(Integer id) {
        ReturnOrder ro = returnOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn trả hàng ID " + id + " không tồn tại"));
        List<ReturnOrderLineItem> items = returnOrderLineItemRepository.findByReturnOrderId(id);
        return mapToResReturnOrderDTO(ro, items);
    }

    public List<ResReturnOrderDTO> getReturnsByOriginalOrderId(Integer orderId) {
        List<ReturnOrder> returns = returnOrderRepository.findByOrderId(orderId);
        return returns.stream().map(ro -> {
            List<ReturnOrderLineItem> items = returnOrderLineItemRepository.findByReturnOrderId(ro.getId());
            return mapToResReturnOrderDTO(ro, items);
        }).toList();
    }

    public ResultPaginationDTO getAllReturns(Pageable pageable, String keyword) {
        Specification<ReturnOrder> spec = ReturnOrderSpecification.build(keyword);
        Page<ReturnOrder> page = returnOrderRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());
        rs.setMeta(meta);

        List<ResReturnOrderDTO> listRes = page.getContent().stream().map(ro -> {
            List<ReturnOrderLineItem> items = returnOrderLineItemRepository.findByReturnOrderId(ro.getId());
            return mapToResReturnOrderDTO(ro, items);
        }).toList();

        rs.setResult(listRes);
        return rs;
    }

    private ResReturnOrderDTO mapToResReturnOrderDTO(ReturnOrder ro, List<ReturnOrderLineItem> items) {
        List<ResReturnOrderDTO.ResReturnOrderItemDTO> itemDTOs = items.stream().map(item -> 
            ResReturnOrderDTO.ResReturnOrderItemDTO.builder()
                    .id(item.getId())
                    .variantId(item.getVariantId())
                    .productName(item.getProductName())
                    .productSku(item.getProductSku())
                    .quantity(item.getQuantity())
                    .refundPrice(item.getRefundPrice())
                    .subtotal(item.getSubtotal())
                    .build()
        ).toList();

        return ResReturnOrderDTO.builder()
                .id(ro.getId())
                .returnNumber(ro.getReturnNumber())
                .originalOrderId(ro.getOrder().getId())
                .originalOrderNumber(ro.getOrder().getOrderNumber())
                .customerId(ro.getCustomerId())
                .customerName(ro.getCustomerName())
                .createdById(ro.getCreatedBy())
                .createdByUsername(ro.getCreatedByUsername())
                .approvedByUsername(ro.getApprovedByUsername())
                .totalRefundAmount(ro.getTotalRefundAmount())
                .reason(ro.getReason())
                .createdAt(ro.getCreatedAt())
                .updatedAt(ro.getUpdatedAt())
                .items(itemDTOs)
                .build();
    }
}
