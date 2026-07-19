package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ReqCancelOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.util.constant.PaymentMethod;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.util.constant.PointConstant;
import com.sapo.mock.clothing.setting.service.SystemSettingService;
import com.sapo.mock.clothing.payment.repository.PaymentLogRepository;
import com.sapo.mock.clothing.entity.PaymentLog;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import com.sapo.mock.clothing.specification.OrderSpecification;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final OrderInventoryService orderInventoryService;
    private final OrderLoyaltyService orderLoyaltyService;
    private final OrderNotificationHelper orderNotificationHelper;
    private final SystemSettingService systemSettingService;
    private final PaymentLogRepository paymentLogRepository;
    private final CustomerVoucherRepository customerVoucherRepository;

    // Tạo đơn hàng mới
    @Transactional
    public ResOrderDTO createOrder(ReqCreateOrderDTO dto, String username) {
        User createdBy = getUserByUsername(username);
        Customer customer = getCustomerById(dto.getCustomerId());

        Order order = new Order();
        initOrderInfo(order, dto, customer, createdBy);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        order.setOrderNumber("HD-" + dateStr + "-" + timeStr);

        // Bug #5 fix: Không cho tạo đơn hàng rỗng
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        CustomerVoucher appliedVoucher = orderLoyaltyService.applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        orderLoyaltyService.applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        // Truyền orderNumber vào deductProductStock để ghi StockLog
        orderInventoryService.deductProductStock(dto.getItems(), null, order.getOrderNumber());

        Order savedOrder = orderRepository.save(order);
        orderLineItemRepository.saveAll(lineItems);
        // Cập nhật lại referenceId trong StockLog sau khi có orderId thật
        // (ghi log 2 lần nếu muốn exact; đơn giản nhất là truyền orderId sau khi save)

        if (order.getStatus() == OrderStatus.PENDING) {
            if (appliedVoucher != null) {
                orderLoyaltyService.reserveVoucher(appliedVoucher, savedOrder.getId());
            }
            orderLoyaltyService.reservePoints(savedOrder, customer);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            orderLoyaltyService.processLoyaltyOnCompletion(savedOrder, customer, appliedVoucher);
            eventPublisher
                    .publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
            orderNotificationHelper.sendOrderNotifications(savedOrder, dto.getPaymentMethod());
        }

        return mapToResOrderDTO(savedOrder, lineItems);
    }

    // Lấy chi tiết đơn hàng
    public ResOrderDTO getOrderById(Integer id) {
        Order order = getOrderEntityById(id);
        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        return mapToResOrderDTO(order, items);
    }

    public Order getOrderEntityByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new com.sapo.mock.clothing.exception.ResourceNotFoundException("Đơn hàng không tồn tại: " + orderNumber));
    }

    // Lấy danh sách đơn hàng
    public ResultPaginationDTO getAllOrders(Pageable pageable, OrderStatus status, String keyword) {
        Specification<Order> spec = OrderSpecification.build(status, keyword);
        Page<Order> pageOrder = orderRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(pageOrder.getTotalPages());
        meta.setTotal(pageOrder.getTotalElements());
        rs.setMeta(meta);

        List<Integer> orderIds = pageOrder.getContent().stream().map(Order::getId).toList();
        List<OrderLineItem> allItems = orderIds.isEmpty() ? new ArrayList<>()
                : orderLineItemRepository.findByOrderIdIn(orderIds);
        Map<Integer, List<OrderLineItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        List<ResOrderDTO> listRes = pageOrder.getContent().stream()
                .map(order -> mapToResOrderDTO(order, itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>())))
                .toList();
        rs.setResult(listRes);
        return rs;
    }

    // Cập nhật đơn hàng
    @Transactional
    public ResOrderDTO updateOrder(Integer id, ReqCreateOrderDTO dto, String username) {
        User createdBy = getUserByUsername(username);
        Order order = getOrderEntityById(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể cập nhật đơn hàng ở trạng thái Đang xử lý");
        }

        Customer customer = getCustomerById(dto.getCustomerId());

        List<OrderLineItem> oldItems = orderLineItemRepository.findByOrderId(id);
        orderInventoryService.restoreProductStock(oldItems, id, order.getOrderNumber());
        orderLineItemRepository.deleteAll(oldItems);

        initOrderInfo(order, dto, customer, createdBy);

        // Bug #5 fix: Không cho cập nhật đơn hàng thành rỗng
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        CustomerVoucher appliedVoucher = orderLoyaltyService.applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        orderLoyaltyService.applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        orderInventoryService.deductProductStock(dto.getItems(), id, order.getOrderNumber());

        Order savedOrder = orderRepository.save(order);
        orderLineItemRepository.saveAll(lineItems);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            orderLoyaltyService.processLoyaltyOnCompletion(savedOrder, customer, appliedVoucher);
            eventPublisher
                    .publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
            orderNotificationHelper.sendOrderNotifications(savedOrder, dto.getPaymentMethod());
        }

        return mapToResOrderDTO(savedOrder, lineItems);
    }

    // Hủy đơn hàng
    @Transactional
    public ResOrderDTO cancelOrder(Integer id, ReqCancelOrderDTO dto, String username) {
        Order order = orderRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));

        if (order.getStatus() != OrderStatus.PENDING && systemSettingService.isCancelApprovalRequired()) {
            if (dto.getApprovalPin() == null || dto.getApprovalPin().isEmpty()) {
                throw new BadRequestException("Cần có mã PIN quản lý để duyệt hủy đơn hàng.");
            }

            // Kiểm tra PIN
            List<User> approvers = userRepository.findAll().stream()
                    .filter(u -> "ROLE_ADMIN".equals(u.getRole().getName()) || "ROLE_MANAGER".equals(u.getRole().getName()))
                    .filter(u -> u.getSecurityPin() != null && u.getSecurityPin().equals(dto.getApprovalPin()))
                    .toList();

            if (approvers.isEmpty()) {
                throw new BadRequestException("Mã PIN không chính xác hoặc người duyệt không có quyền.");
            }
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng này đã bị hủy trước đó");
        }
        if (order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.PARTIALLY_RETURNED) {
            throw new BadRequestException("Không thể hủy đơn hàng đã có trả hàng. Vui lòng xử lý qua phiếu trả hàng.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(dto.getReason());
        order.setPaidAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        Customer customer = getCustomerById(order.getCustomerId());

        if (previousStatus == OrderStatus.COMPLETED || previousStatus == OrderStatus.PENDING) {
            orderLoyaltyService.revertLoyaltyOnCancel(savedOrder, customer);
        }

        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        orderInventoryService.restoreProductStock(items, id, order.getOrderNumber());

        return mapToResOrderDTO(savedOrder, items);
    }

    // Đánh dấu đơn hàng đã in
    @Transactional
    public ResOrderDTO updatePrintStatus(Integer id, boolean status) {
        Order order = getOrderEntityById(id);
        order.setPrinted(status);
        orderRepository.save(order);
        return mapToResOrderDTO(order, orderLineItemRepository.findByOrderId(id));
    }

    // Lấy thông tin người dùng
    private User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null)
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        return user;
    }

    // Lấy thông tin khách hàng
    private Customer getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng ID " + id + " không tồn tại"));
    }

    // Lấy thông tin đơn hàng
    private Order getOrderEntityById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
    }

    // Khởi tạo thông tin đơn hàng
    private void initOrderInfo(Order order, ReqCreateOrderDTO dto, Customer customer, User createdBy) {
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getFullName());
        order.setCreatedBy(createdBy.getId());
        order.setCreatedByUsername(createdBy.getUsername());
        order.setNote(dto.getNote());
        order.setPaidAmount(dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO);
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : OrderStatus.COMPLETED);
        order.setPaymentMethod(dto.getPaymentMethod() != null ? PaymentMethod.valueOf(dto.getPaymentMethod()) : PaymentMethod.CASH);
        if (order.getId() == null)
            order.setPrinted(false);
    }

    // Xây dựng danh sách sản phẩm trong đơn hàng
    private BigDecimal buildOrderItems(List<ReqCreateOrderDTO.OrderItemDTO> itemsDto, Order order,
            List<OrderLineItem> lineItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : itemsDto) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm ID " + itemDto.getVariantId() + " không tồn tại"));

            BigDecimal unitPrice = variant.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderLineItem lineItem = new OrderLineItem();
            lineItem.setVariantId(variant.getId());

            String fullName = variant.getProduct().getName();
            List<String> options = new ArrayList<>();
            if (variant.getOption1Value() != null)
                options.add(variant.getOption1Value().getValue());
            if (variant.getOption2Value() != null)
                options.add(variant.getOption2Value().getValue());
            if (variant.getOption3Value() != null)
                options.add(variant.getOption3Value().getValue());
            if (!options.isEmpty())
                fullName += " (" + String.join(" - ", options) + ")";

            lineItem.setProductName(fullName);
            lineItem.setProductSku(variant.getSku());
            lineItem.setQuantity(itemDto.getQuantity());
            lineItem.setUnitPrice(unitPrice);
            lineItem.setSubtotal(subtotal);
            lineItem.setOrder(order);
            lineItems.add(lineItem);
        }
        return totalAmount;
    }

    // Kết thúc đơn hàng
    private void finalizeOrderAmounts(ReqCreateOrderDTO dto, Order order, BigDecimal totalAmount) {
        order.setTotalAmount(totalAmount);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            BigDecimal paid = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;
            if (paid.compareTo(totalAmount) < 0) {
                throw new BadRequestException(
                        "Số tiền khách đưa (" + paid + ") không đủ để thanh toán đơn hàng (" + totalAmount + ")");
            }
            order.setChangeAmount(paid.subtract(totalAmount));
            order.setPointsEarned(order.getCustomerId() == 1 ? 0
                    : totalAmount.divideToIntegralValue(PointConstant.EARN_RATE).intValue());
        } else {
            order.setChangeAmount(BigDecimal.ZERO);
            order.setPointsEarned(0);
        }
    }

    // Hoàn thành thanh toán đơn hàng bằng QR_SEPAY
    @Transactional
    public void completeOrderPayment(String orderNumber, BigDecimal paidAmount) {
        Order order = orderRepository.findByOrderNumberWithPessimisticLock(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        if (paidAmount.compareTo(order.getTotalAmount()) < 0) {
            orderNotificationHelper.sendPaymentFailureNotification(order, paidAmount);
            throw new BadRequestException("Số tiền thanh toán không đủ");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentMethod(PaymentMethod.QR_SEPAY);
        order.setPaidAmount(paidAmount);
        order.setChangeAmount(paidAmount.subtract(order.getTotalAmount()));
        order.setPointsEarned(order.getCustomerId() == 1 ? 0
                : order.getTotalAmount().divideToIntegralValue(PointConstant.EARN_RATE).intValue());

        Order savedOrder = orderRepository.save(order);
        Customer customer = getCustomerById(savedOrder.getCustomerId());

        CustomerVoucher appliedVoucher = orderLoyaltyService.getAppliedVoucher(savedOrder.getId());

        orderLoyaltyService.processLoyaltyOnCompletion(savedOrder, customer, appliedVoucher);
        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
        orderNotificationHelper.sendOrderNotifications(savedOrder, "QR_SEPAY");
    }

    // Hoàn thành thanh toán đơn hàng bằng ID (sử dụng cho luồng POS thủ công/online tối giản)
    @Transactional
    public ResOrderDTO completeOrderPaymentById(Integer id, String paymentMethod, BigDecimal paidAmount) {
        Order order = orderRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
        if (order.getStatus() != OrderStatus.PENDING) {
            return mapToResOrderDTO(order, orderLineItemRepository.findByOrderId(id));
        }

        BigDecimal amount = paidAmount != null ? paidAmount : order.getTotalAmount();
        if (amount.compareTo(order.getTotalAmount()) < 0) {
            throw new BadRequestException("Số tiền thanh toán không đủ");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentMethod(paymentMethod != null ? PaymentMethod.valueOf(paymentMethod) : PaymentMethod.CASH);
        order.setPaidAmount(amount);
        order.setChangeAmount(amount.subtract(order.getTotalAmount()));
        order.setPointsEarned(order.getCustomerId() == 1 ? 0
                : order.getTotalAmount().divideToIntegralValue(PointConstant.EARN_RATE).intValue());

        Order savedOrder = orderRepository.save(order);
        Customer customer = getCustomerById(savedOrder.getCustomerId());
        CustomerVoucher appliedVoucher = orderLoyaltyService.getAppliedVoucher(savedOrder.getId());
        
        orderLoyaltyService.processLoyaltyOnCompletion(savedOrder, customer, appliedVoucher);
        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
        
        if ("QR_SEPAY".equals(paymentMethod)) {
            PaymentLog paymentLog = new PaymentLog();
            paymentLog.setReferenceCode("MANUAL_" + System.currentTimeMillis());
            paymentLog.setOrderNumber(savedOrder.getOrderNumber());
            paymentLog.setTransferAmount(amount);
            paymentLog.setGateway("MANUAL");
            paymentLog.setTransactionDate(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            paymentLog.setContent("Thanh toán QR được xác nhận thủ công cho đơn " + savedOrder.getOrderNumber());
            paymentLog.setStatus("SUCCESS");
            paymentLogRepository.save(paymentLog);
        }

        orderNotificationHelper.sendOrderNotifications(savedOrder, paymentMethod);

        return mapToResOrderDTO(savedOrder, orderLineItemRepository.findByOrderId(savedOrder.getId()));
    }

    private BigDecimal getVoucherMinOrderValue(Order order) {
        if (order.getVoucherCode() == null) return null;
        return customerVoucherRepository.findByOrderIdWithVoucher(order.getId())
                .map(cv -> cv.getVoucher() != null ? cv.getVoucher().getMinOrderValue() : null)
                .orElse(null);
    }

    // Mapping đơn hàng sang ResOrderDTO
    private ResOrderDTO mapToResOrderDTO(Order savedOrder, List<OrderLineItem> lineItems) {
        List<ResOrderDTO.ResOrderItemDTO> resItems = lineItems.stream()
                .map(i -> ResOrderDTO.ResOrderItemDTO.builder()
                        .id(i.getId())
                        .variantId(i.getVariantId())
                        .productName(i.getProductName())
                        .productSku(i.getProductSku())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return ResOrderDTO.builder()
                .id(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .customerId(savedOrder.getCustomerId())
                .customerName(savedOrder.getCustomerName())
                .createdById(savedOrder.getCreatedBy())
                .createdByUsername(savedOrder.getCreatedByUsername())
                .totalAmount(savedOrder.getTotalAmount())
                .paidAmount(savedOrder.getPaidAmount())
                .changeAmount(savedOrder.getChangeAmount())
                .pointsUsed(savedOrder.getPointsUsed())
                .pointsEarned(savedOrder.getPointsEarned())
                .discountFromPoints(savedOrder.getDiscountFromPoints())
                .voucherCode(savedOrder.getVoucherCode())
                .discountFromVoucher(savedOrder.getDiscountFromVoucher())
                .voucherMinOrderValue(getVoucherMinOrderValue(savedOrder))
                .status(savedOrder.getStatus())
                .paymentMethod(savedOrder.getPaymentMethod())
                .isPrinted(savedOrder.isPrinted())
                .note(savedOrder.getNote())
                .createdAt(savedOrder.getCreatedAt())
                .updatedAt(savedOrder.getUpdatedAt())
                .items(resItems)
                .build();
    }
}
