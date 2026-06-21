package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import com.sapo.mock.clothing.util.constant.PointConstant;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final PointHistoryRepository pointHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ResOrderDTO createOrder(ReqCreateOrderDTO dto, String username) {

        User createdBy = userRepository.findByUsername(username);
        if (createdBy == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng ID " + dto.getCustomerId() + " không tồn tại"));

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getFullName());
        order.setCreatedBy(createdBy.getId());
        order.setCreatedByUsername(createdBy.getUsername());
        order.setNote(dto.getNote());
        order.setPaidAmount(dto.getPaidAmount());
        order.setStatus(OrderStatus.COMPLETED);
        order.setPrinted(false);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long countToday = orderRepository.countByCreatedAtAfter(startOfDay);
        String orderNumber = "HD-" + dateStr + "-" + String.format("%03d", countToday + 1);
        order.setOrderNumber(orderNumber);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderLineItem> lineItems = new ArrayList<>();

        for (ReqCreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm ID " + itemDto.getVariantId() + " không tồn tại"));

            BigDecimal unitPrice = variant.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderLineItem lineItem = new OrderLineItem();
            lineItem.setVariantId(variant.getId());
            
            // Build full product name with options
            String fullName = variant.getProduct().getName();
            List<String> options = new ArrayList<>();
            if (variant.getOption1Value() != null) options.add(variant.getOption1Value().getValue());
            if (variant.getOption2Value() != null) options.add(variant.getOption2Value().getValue());
            if (variant.getOption3Value() != null) options.add(variant.getOption3Value().getValue());
            if (!options.isEmpty()) {
                fullName += " (" + String.join(" - ", options) + ")";
            }
            lineItem.setProductName(fullName);
            lineItem.setProductSku(variant.getSku());
            lineItem.setQuantity(itemDto.getQuantity());
            lineItem.setUnitPrice(unitPrice);
            lineItem.setSubtotal(subtotal);
            lineItem.setOrder(order);

            lineItems.add(lineItem);
        }

        if (dto.getPointsToUse() != null && dto.getPointsToUse() > 0) {
            if (customer.getRewardPoints() < dto.getPointsToUse()) {
                throw new BadRequestException("Khách hàng không đủ điểm. Điểm hiện tại: " + customer.getRewardPoints());
            }
            
            BigDecimal discount = BigDecimal.valueOf(dto.getPointsToUse()).multiply(PointConstant.REDEEM_RATE);
            if (discount.compareTo(totalAmount) > 0) {
                discount = totalAmount; // Không giảm quá tổng tiền
            }
            
            order.setPointsUsed(dto.getPointsToUse());
            order.setDiscountFromPoints(discount);
            totalAmount = totalAmount.subtract(discount);
        }

        if (dto.getPaidAmount().compareTo(totalAmount) < 0) {
            throw new BadRequestException(
                    "Số tiền khách đưa (" + dto.getPaidAmount() + ") không đủ để thanh toán đơn hàng (" + totalAmount + ")");
        }

        order.setTotalAmount(totalAmount);
        order.setChangeAmount(dto.getPaidAmount().subtract(totalAmount));

        // Tích điểm cho đơn hàng
        int earnedPoints = totalAmount.divideToIntegralValue(PointConstant.EARN_RATE).intValue();
        order.setPointsEarned(earnedPoints);

        this.deductProductStock(dto.getItems());

        Order savedOrder = orderRepository.save(order);
        orderLineItemRepository.saveAll(lineItems);

        // Lưu lịch sử và cập nhật điểm khách hàng
        if (order.getPointsUsed() > 0) {
            customer.setRewardPoints(customer.getRewardPoints() - order.getPointsUsed());
            PointHistory phUse = new PointHistory();
            phUse.setCustomerId(customer.getId());
            phUse.setOrderId(savedOrder.getId());
            phUse.setPointsChange(-order.getPointsUsed());
            phUse.setType(PointConstant.TYPE_REDEEM);
            phUse.setDescription("Sử dụng điểm cho đơn hàng " + savedOrder.getOrderNumber());
            pointHistoryRepository.save(phUse);
        }
        
        if (order.getPointsEarned() > 0) {
            customer.setRewardPoints(customer.getRewardPoints() + order.getPointsEarned());
            PointHistory phEarn = new PointHistory();
            phEarn.setCustomerId(customer.getId());
            phEarn.setOrderId(savedOrder.getId());
            phEarn.setPointsChange(order.getPointsEarned());
            phEarn.setType(PointConstant.TYPE_EARN);
            phEarn.setDescription("Tích điểm từ đơn hàng " + savedOrder.getOrderNumber());
            pointHistoryRepository.save(phEarn);
        }
        customerRepository.save(customer);

        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
        return mapToResOrderDTO(savedOrder, lineItems);
    }

    public ResOrderDTO getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);

        return mapToResOrderDTO(order, items);
    }

    public ResultPaginationDTO getAllOrders(Pageable pageable) {
        Page<Order> pageOrder = orderRepository.findAll(pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(pageOrder.getTotalPages());
        meta.setTotal(pageOrder.getTotalElements());

        rs.setMeta(meta);

        List<Integer> orderIds = pageOrder.getContent().stream()
                .map(Order::getId)
                .toList();

        List<OrderLineItem> allItems = orderIds.isEmpty() ? new ArrayList<>()
                : orderLineItemRepository.findByOrderIdIn(orderIds);

        Map<Integer, List<OrderLineItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        List<ResOrderDTO> listRes = pageOrder.getContent().stream()
                .map(order -> {
                    List<OrderLineItem> items = itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>());
                    return mapToResOrderDTO(order, items);
                })
                .toList();

        rs.setResult(listRes);
        return rs;
    }

    @Transactional
    public ResOrderDTO cancelOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng này đã bị hủy trước đó");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại"));

        // Hoàn trả / trừ lại điểm
        if (order.getPointsUsed() > 0) {
            customer.setRewardPoints(customer.getRewardPoints() + order.getPointsUsed());
            PointHistory phRefund = new PointHistory();
            phRefund.setCustomerId(customer.getId());
            phRefund.setOrderId(savedOrder.getId());
            phRefund.setPointsChange(order.getPointsUsed());
            phRefund.setType(PointConstant.TYPE_REFUND);
            phRefund.setDescription("Hoàn điểm do hủy đơn hàng " + savedOrder.getOrderNumber());
            pointHistoryRepository.save(phRefund);
        }
        if (order.getPointsEarned() > 0) {
            customer.setRewardPoints(customer.getRewardPoints() - order.getPointsEarned());
            PointHistory phRevertEarn = new PointHistory();
            phRevertEarn.setCustomerId(customer.getId());
            phRevertEarn.setOrderId(savedOrder.getId());
            phRevertEarn.setPointsChange(-order.getPointsEarned());
            phRevertEarn.setType(PointConstant.TYPE_REFUND);
            phRevertEarn.setDescription("Trừ điểm tích lũy do hủy đơn hàng " + savedOrder.getOrderNumber());
            pointHistoryRepository.save(phRevertEarn);
        }
        customerRepository.save(customer);

        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        for (OrderLineItem item : items) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy thông tin tồn kho của sản phẩm ID " + item.getVariantId()));

            variant.setQuantity(variant.getQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        return mapToResOrderDTO(savedOrder, items);
    }

    @Transactional
    public ResOrderDTO updatePrintStatus(Integer id, boolean status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));

        order.setPrinted(status);
        Order savedOrder = orderRepository.save(order);

        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        return mapToResOrderDTO(savedOrder, items);
    }

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
                .status(savedOrder.getStatus())
                .isPrinted(savedOrder.isPrinted())
                .note(savedOrder.getNote())
                .createdAt(savedOrder.getCreatedAt())
                .updatedAt(savedOrder.getUpdatedAt())
                .items(resItems)
                .build();
    }

    private void deductProductStock(List<ReqCreateOrderDTO.OrderItemDTO> items) {
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : items) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm ID " + itemDto.getVariantId()));

            if (variant.getQuantity() < itemDto.getQuantity()) {
                throw new BadRequestException("Sản phẩm ID " + itemDto.getVariantId()
                        + " không đủ số lượng (Hiện có: " + variant.getQuantity() + ")");
            }

            variant.setQuantity(variant.getQuantity() - itemDto.getQuantity());
            productVariantRepository.save(variant);
        }
    }
}
