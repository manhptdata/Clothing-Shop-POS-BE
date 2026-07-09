package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.util.constant.PointConstant;

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

    // Tạo đơn hàng mới
    @Transactional
    public ResOrderDTO createOrder(ReqCreateOrderDTO dto, String username) {
        User createdBy = getUserByUsername(username);
        Customer customer = getCustomerById(dto.getCustomerId());

        Order order = new Order();
        initOrderInfo(order, dto, customer, createdBy);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long countToday = orderRepository
                .countByCreatedAtAfter(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        order.setOrderNumber("HD-" + dateStr + "-" + String.format("%03d", countToday + 1));

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        CustomerVoucher appliedVoucher = orderLoyaltyService.applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        orderLoyaltyService.applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        orderInventoryService.deductProductStock(dto.getItems());

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

    // Lấy chi tiết đơn hàng
    public ResOrderDTO getOrderById(Integer id) {
        Order order = getOrderEntityById(id);
        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        return mapToResOrderDTO(order, items);
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
        orderInventoryService.restoreProductStock(oldItems);
        orderLineItemRepository.deleteAll(oldItems);

        initOrderInfo(order, dto, customer, createdBy);

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        CustomerVoucher appliedVoucher = orderLoyaltyService.applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        orderLoyaltyService.applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        orderInventoryService.deductProductStock(dto.getItems());

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
    public ResOrderDTO cancelOrder(Integer id) {
        Order order = getOrderEntityById(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng này đã bị hủy trước đó");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        Customer customer = getCustomerById(order.getCustomerId());

        if (previousStatus == OrderStatus.COMPLETED) {
            orderLoyaltyService.revertLoyaltyOnCancel(savedOrder, customer);
        }

        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        orderInventoryService.restoreProductStock(items);

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
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        if (paidAmount.compareTo(order.getTotalAmount()) < 0) {
            orderNotificationHelper.sendPaymentFailureNotification(order, paidAmount);
            throw new BadRequestException("Số tiền thanh toán không đủ");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPaidAmount(paidAmount);
        order.setChangeAmount(paidAmount.subtract(order.getTotalAmount()));
        order.setPointsEarned(order.getCustomerId() == 1 ? 0
                : order.getTotalAmount().divideToIntegralValue(PointConstant.EARN_RATE).intValue());

        Order savedOrder = orderRepository.save(order);
        Customer customer = getCustomerById(savedOrder.getCustomerId());

        CustomerVoucher appliedVoucher = orderLoyaltyService.getAppliedVoucher(customer.getId(),
                savedOrder.getVoucherCode());

        orderLoyaltyService.processLoyaltyOnCompletion(savedOrder, customer, appliedVoucher);
        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
        orderNotificationHelper.sendOrderNotifications(savedOrder, "QR_SEPAY");
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
                .status(savedOrder.getStatus())
                .isPrinted(savedOrder.isPrinted())
                .note(savedOrder.getNote())
                .createdAt(savedOrder.getCreatedAt())
                .updatedAt(savedOrder.getUpdatedAt())
                .items(resItems)
                .build();
    }
}
