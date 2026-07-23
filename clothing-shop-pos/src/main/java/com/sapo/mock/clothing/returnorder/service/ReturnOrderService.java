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
        if (!createdBy.isActive()) {
            throw new BadRequestException("Tài khoản nhân viên '" + username + "' đã bị vô hiệu hóa hoặc bị khóa.");
        }

        String approvedByUsername = createdBy.getUsername();

        // 0. Verify PIN if required
        if (systemSettingService.isReturnApprovalRequired()) {
            if (dto.getApprovalPin() == null || dto.getApprovalPin().isEmpty()) {
                throw new BadRequestException("Cần có mã PIN quản lý để duyệt phiếu trả hàng.");
            }

            // Find an admin/manager who matches this PIN
            List<User> approvers = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && ("ROLE_ADMIN".equals(u.getRole().getName())
                            || "ROLE_MANAGER".equals(u.getRole().getName())))
                    .filter(u -> u.getSecurityPin() != null
                            && passwordEncoder.matches(dto.getApprovalPin(), u.getSecurityPin()))
                    .toList();

            if (approvers.isEmpty()) {
                throw new BadRequestException("Mã PIN không chính xác hoặc người duyệt không có quyền.");
            }
            approvedByUsername = approvers.get(0).getUsername();
        }

        Order order = orderRepository.findByIdWithPessimisticLock(dto.getOriginalOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hóa đơn gốc ID " + dto.getOriginalOrderId() + " không tồn tại"));

        // 1. Kiểm tra trạng thái đơn hàng gốc
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.PARTIALLY_RETURNED) {
            throw new BadRequestException(
                    "Chỉ được trả hàng cho hóa đơn có trạng thái Hoàn thành hoặc Trả hàng một phần");
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
                alreadyReturnedMap.put(item.getVariantId(),
                        alreadyReturnedMap.getOrDefault(item.getVariantId(), 0) + item.getQuantity());
            }
        }

        // 3. Tính toán chiết khấu tỷ lệ của đơn hàng gốc
        BigDecimal totalDiscount = (order.getDiscountFromVoucher() != null ? order.getDiscountFromVoucher()
                : BigDecimal.ZERO)
                .add(order.getDiscountFromPoints() != null ? order.getDiscountFromPoints() : BigDecimal.ZERO);

        BigDecimal originalSubtotal = BigDecimal.ZERO;
        for (OrderLineItem item : originalItems) {
            originalSubtotal = originalSubtotal
                    .add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
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
        returnOrder.setRefundMethod(dto.getRefundMethod() != null ? dto.getRefundMethod() : "CASH");
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
            int totalReturnedForVariant = alreadyReturnedMap.getOrDefault(originalItem.getVariantId(), 0)
                    + currentReturnedInDto;
            if (totalReturnedForVariant < originalItem.getQuantity()) {
                isAllReturned = false;
                break;
            }
        }

        List<ReturnOrderLineItem> returnLineItems = new ArrayList<>();
        BigDecimal computedRefundAmount = BigDecimal.ZERO;
        BigDecimal returnedItemsSubtotal = BigDecimal.ZERO;

        // Anti-deadlock: Sắp xếp theo variantId tăng dần để luôn lock theo thứ tự cố
        // định
        List<ReqCreateReturnDTO.ReturnItemDTO> sortedReturnItems = new ArrayList<>(dto.getItems());
        sortedReturnItems.sort(java.util.Comparator.comparing(ReqCreateReturnDTO.ReturnItemDTO::getVariantId));

        for (ReqCreateReturnDTO.ReturnItemDTO returnItemDto : sortedReturnItems) {
            OrderLineItem originalItem = originalItemsMap.get(returnItemDto.getVariantId());
            if (originalItem == null) {
                throw new BadRequestException(
                        "Sản phẩm ID " + returnItemDto.getVariantId() + " không có trong hóa đơn gốc.");
            }

            int alreadyReturned = alreadyReturnedMap.getOrDefault(returnItemDto.getVariantId(), 0);
            int remainingAllowed = originalItem.getQuantity() - alreadyReturned;

            if (returnItemDto.getQuantity() > remainingAllowed) {
                throw new BadRequestException("Sản phẩm " + originalItem.getProductName() + " đã trả " + alreadyReturned
                        + " chiếc. Số lượng muốn trả tiếp (" + returnItemDto.getQuantity()
                        + ") vượt quá số lượng còn lại có thể trả (" + remainingAllowed + ").");
            }

            alreadyReturnedMap.put(returnItemDto.getVariantId(), alreadyReturned + returnItemDto.getQuantity());

            ProductVariant variant = productVariantRepository.findByIdWithPessimisticLock(returnItemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm phân loại ID " + returnItemDto.getVariantId() + " không tồn tại"));

            boolean shouldRestock = returnItemDto.getRestock() == null || returnItemDto.getRestock();
            int qtyBefore = variant.getQuantity();
            int qtyAfter = qtyBefore;

            if (shouldRestock) {
                qtyAfter = qtyBefore + returnItemDto.getQuantity();
                variant.setQuantity(qtyAfter);
                productVariantRepository.save(variant);

                StockLog stockLog = new StockLog();
                stockLog.setVariant(variant);
                stockLog.setQuantityBefore(qtyBefore);
                stockLog.setQuantityChange(returnItemDto.getQuantity());
                stockLog.setQuantityAfter(qtyAfter);
                stockLog.setSource(StockLogSource.TRA_HANG);
                stockLog.setReferenceType(StockLogReferenceType.RETURN);
                stockLog.setReferenceId(order.getId());
                stockLog.setNote("Khách trả hàng đơn " + order.getOrderNumber() + " (Nhập lại kho)");
                stockLog.setCreatedBy(createdBy);
                stockLogRepository.save(stockLog);
            } else {
                StockLog stockLog = new StockLog();
                stockLog.setVariant(variant);
                stockLog.setQuantityBefore(qtyBefore);
                stockLog.setQuantityChange(0);
                stockLog.setQuantityAfter(qtyBefore);
                stockLog.setSource(StockLogSource.TRA_HANG);
                stockLog.setReferenceType(StockLogReferenceType.RETURN);
                stockLog.setReferenceId(order.getId());
                stockLog.setNote("Khách trả hàng hỏng/lỗi đơn " + order.getOrderNumber() + " (Không nhập lại kho)");
                stockLog.setCreatedBy(createdBy);
                stockLogRepository.save(stockLog);
            }

            // Tính tổng tiền hoàn lại của món này (làm tròn ở bước cuối cùng để tránh sai
            // số)
            BigDecimal unitPrice = originalItem.getUnitPrice();
            BigDecimal refundPrice = unitPrice.multiply(BigDecimal.ONE.subtract(discountRatio));
            BigDecimal itemSubtotal = refundPrice.multiply(BigDecimal.valueOf(returnItemDto.getQuantity()));

            ReturnOrderLineItem returnLineItem = new ReturnOrderLineItem();
            returnLineItem.setReturnOrder(returnOrder);
            returnLineItem.setVariantId(variant.getId());
            returnLineItem.setProductName(originalItem.getProductName());
            returnLineItem.setProductSku(originalItem.getProductSku());
            returnLineItem.setQuantity(returnItemDto.getQuantity());
            returnLineItem.setRefundPrice(refundPrice.setScale(0, RoundingMode.HALF_UP));
            returnLineItem.setSubtotal(itemSubtotal.setScale(0, RoundingMode.HALF_UP));
            returnLineItem.setRestocked(shouldRestock);

            returnLineItems.add(returnLineItem);
            computedRefundAmount = computedRefundAmount.add(itemSubtotal);
            returnedItemsSubtotal = returnedItemsSubtotal
                    .add(originalItem.getUnitPrice().multiply(BigDecimal.valueOf(returnItemDto.getQuantity())));
        }

        // --- 1. TÍNH TỔNG GIÁ TRỊ HÀNG HÓA ĐÃ TRẢ Ở CÁC LẦN TRƯỚC (NẾU CÓ) ---
        BigDecimal previousReturnedSubtotal = BigDecimal.ZERO;
        for (ReturnOrder ro : existingReturns) {
            for (ReturnOrderLineItem item : ro.getItems()) {
                OrderLineItem origItem = originalItemsMap.get(item.getVariantId());
                if (origItem != null) {
                    previousReturnedSubtotal = previousReturnedSubtotal
                            .add(origItem.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        // --- 2. THUẬT TOÁN FAIR PRICE (GIÁ TRỊ CÔNG BẰNG) CHUẨN E-COMMERCE ---
        
        // A. Giá trị nguyên bản (giá niêm yết) của phần hàng khách GIỮ LẠI sau lần trả này
        BigDecimal newSubtotal = originalSubtotal.subtract(previousReturnedSubtotal).subtract(returnedItemsSubtotal);
        if (newSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            newSubtotal = BigDecimal.ZERO;
        }

        // B. Tính Giá trị công bằng (Fair Price) của phần hàng GIỮ LẠI ở LẦN TRẢ NÀY
        BigDecimal fairPriceOfKeptItems = calculateFairPriceForSubtotal(order, originalSubtotal, newSubtotal);

        // C. Tính Giá trị công bằng (Fair Price) của phần hàng GIỮ LẠI ở LẦN TRẢ TRƯỚC (Dùng cho khấu trừ điểm)
        BigDecimal previousKeptSubtotal = originalSubtotal.subtract(previousReturnedSubtotal);
        if (previousKeptSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            previousKeptSubtotal = BigDecimal.ZERO;
        }
        BigDecimal previousFairPriceOfKeptItems = calculateFairPriceForSubtotal(order, originalSubtotal, previousKeptSubtotal);

        // D. Tính số tiền thực tế khách ĐÃ ĐÓNG cho toàn bộ đơn hàng (True Paid Amount)
        BigDecimal truePaidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        if (order.getChangeAmount() != null && order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            truePaidAmount = truePaidAmount.subtract(order.getChangeAmount());
        }

        // E. Tổng số tiền CẦN hoàn lại qua tất cả các đợt trả hàng
        BigDecimal totalExpectedRefund = truePaidAmount.subtract(fairPriceOfKeptItems);

        // F. Lấy tổng số tiền ĐÃ HOÀN ở các đợt trước
        BigDecimal previousRefundTotal = returnOrderRepository.getTotalRefundedByOrderId(order.getId());
        if (previousRefundTotal == null) {
            previousRefundTotal = BigDecimal.ZERO;
        }

        // G. Số tiền thực tế hoàn cho khách ở đợt trả này
        BigDecimal actualRefundForThisTurn = totalExpectedRefund.subtract(previousRefundTotal);
        if (actualRefundForThisTurn.compareTo(BigDecimal.ZERO) < 0) {
            actualRefundForThisTurn = BigDecimal.ZERO;
        }

        computedRefundAmount = actualRefundForThisTurn;
        BigDecimal newOrderValue = fairPriceOfKeptItems; // Cập nhật để dùng bên dưới

        // --- 3. KHẤU TRỪ ĐIỂM VÀ DOANH SỐ CHI TIÊU CỦA KHÁCH HÀNG ---
        if (customer.getId() != 1) {
            Customer lockedCustomer = customerRepository.findByIdWithPessimisticLock(customer.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customer.getId()));

            // 3.1 Hoàn lại điểm tiêu dùng theo tỷ lệ phần hàng trả
            if (order.getPointsUsed() > 0 && originalSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal returnRatio = returnedItemsSubtotal.divide(originalSubtotal, 4, RoundingMode.HALF_UP);
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

            // 3.2 Khấu trừ điểm tích lũy dựa trên Fair Price đồng bộ
            int previousEarnedPoints = previousFairPriceOfKeptItems.divideToIntegralValue(PointConstant.EARN_RATE).intValue();
            int previousTheoreticalDeducted = order.getPointsEarned() - previousEarnedPoints;
            if (previousTheoreticalDeducted < 0) previousTheoreticalDeducted = 0;

            int currentEarnedPoints = newOrderValue.divideToIntegralValue(PointConstant.EARN_RATE).intValue();
            int currentTheoreticalDeductedTotal = order.getPointsEarned() - currentEarnedPoints;
            if (currentTheoreticalDeductedTotal < 0) currentTheoreticalDeductedTotal = 0;

            int pointsToDeduct = currentTheoreticalDeductedTotal - previousTheoreticalDeducted;
            if (pointsToDeduct > 0) {
                int currentPoints = lockedCustomer.getRewardPoints();
                if (currentPoints < pointsToDeduct) {
                    lockedCustomer.setRewardPoints(0);

                    PointHistory phDeduct = new PointHistory();
                    phDeduct.setCustomerId(lockedCustomer.getId());
                    phDeduct.setOrderId(order.getId());
                    phDeduct.setPointsChange(-currentPoints);
                    phDeduct.setType(PointConstant.TYPE_REFUND);
                    phDeduct.setDescription("Khấu trừ toàn bộ điểm hiện có do khách trả hàng đơn " + order.getOrderNumber());
                    pointHistoryRepository.save(phDeduct);

                    // Trừ phần điểm thâm hụt trực tiếp vào số tiền thối cho khách
                    int pointsDeficit = pointsToDeduct - currentPoints;
                    BigDecimal moneyToDeduct = BigDecimal.valueOf(pointsDeficit).multiply(PointConstant.REDEEM_RATE);
                    computedRefundAmount = computedRefundAmount.subtract(moneyToDeduct);
                    if (computedRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
                        computedRefundAmount = BigDecimal.ZERO;
                    }
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

            // 3.3 Cập nhật tổng chi tiêu của khách (Dùng con số tiền thối thực tế computedRefundAmount)
            eventPublisher.publishEvent(new OrderCompletedEvent(lockedCustomer.getId(), computedRefundAmount.negate()));
        }

        computedRefundAmount = computedRefundAmount.setScale(0, RoundingMode.HALF_UP);
        returnOrder.setTotalRefundAmount(computedRefundAmount);

        // Lưu đơn trả hàng
        ReturnOrder savedReturnOrder = returnOrderRepository.save(returnOrder);
        returnOrderLineItemRepository.saveAll(returnLineItems);

        // Cập nhật trạng thái đơn hàng gốc

        if (isAllReturned) {
            order.setStatus(OrderStatus.RETURNED);
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
        List<ResReturnOrderDTO.ResReturnOrderItemDTO> itemDTOs = items.stream()
                .map(item -> ResReturnOrderDTO.ResReturnOrderItemDTO.builder()
                        .id(item.getId())
                        .variantId(item.getVariantId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .refundPrice(item.getRefundPrice())
                        .subtotal(item.getSubtotal())
                        .isRestocked(item.isRestocked())
                        .build())
                .toList();

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
                .refundMethod(ro.getRefundMethod())
                .reason(ro.getReason())
                .createdAt(ro.getCreatedAt())
                .updatedAt(ro.getUpdatedAt())
                .items(itemDTOs)
                .build();
    }

    private BigDecimal calculateFairPriceForSubtotal(Order order, BigDecimal originalSubtotal, BigDecimal keptSubtotal) {
        if (keptSubtotal.compareTo(BigDecimal.ZERO) <= 0 || originalSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 1. Giảm giá từ Điểm thưởng cho phần hàng giữ lại
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (order.getPointsUsed() > 0) {
            pointsDiscount = BigDecimal.valueOf(order.getPointsUsed())
                    .multiply(PointConstant.REDEEM_RATE)
                    .multiply(keptSubtotal)
                    .divide(originalSubtotal, 0, RoundingMode.HALF_UP);
        }

        // 2. Giảm giá từ Voucher cho phần hàng giữ lại
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
            BigDecimal minOrderValue = null;
            com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher = customerVoucherRepository
                    .findByOrderId(order.getId()).orElse(null);
            if (appliedVoucher != null && appliedVoucher.getVoucher() != null) {
                minOrderValue = appliedVoucher.getVoucher().getMinOrderValue();
            } else {
                com.sapo.mock.clothing.entity.Voucher publicVoucher = voucherRepository
                        .findByCode(order.getVoucherCode()).orElse(null);
                if (publicVoucher != null) {
                    minOrderValue = publicVoucher.getMinOrderValue();
                }
            }

            // ĐỦ ĐIỀU KIỆN MIN ORDER VALUE -> Được giữ tỷ lệ giảm giá Voucher
            if (minOrderValue == null || keptSubtotal.compareTo(minOrderValue) >= 0) {
                if (order.getDiscountFromVoucher() != null) {
                    voucherDiscount = order.getDiscountFromVoucher()
                            .multiply(keptSubtotal)
                            .divide(originalSubtotal, 0, RoundingMode.HALF_UP);
                }
            }
            // RỚT ĐIỀU KIỆN -> voucherDiscount tự động = 0 (khách mất hoàn toàn giảm giá Voucher)
        }

        BigDecimal fairPrice = keptSubtotal.subtract(pointsDiscount).subtract(voucherDiscount);
        return fairPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : fairPrice;
        }
}
