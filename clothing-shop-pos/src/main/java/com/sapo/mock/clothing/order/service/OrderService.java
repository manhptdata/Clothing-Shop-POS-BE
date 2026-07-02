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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import com.sapo.mock.clothing.specification.OrderSpecification;

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
    private final com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository customerVoucherRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.sapo.mock.clothing.notification.service.NotificationService notificationService;

    @Transactional
    public ResOrderDTO createOrder(ReqCreateOrderDTO dto, String username) {
        User createdBy = getUserByUsername(username);
        Customer customer = getCustomerById(dto.getCustomerId());

        Order order = new Order();
        initOrderInfo(order, dto, customer, createdBy);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long countToday = orderRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        order.setOrderNumber("HD-" + dateStr + "-" + String.format("%03d", countToday + 1));

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher = applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        deductProductStock(dto.getItems());

        Order savedOrder = orderRepository.save(order);
        orderLineItemRepository.saveAll(lineItems);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            processOrderCompletion(savedOrder, customer, appliedVoucher, dto.getPaymentMethod());
        }

        return mapToResOrderDTO(savedOrder, lineItems);
    }

    public ResOrderDTO getOrderById(Integer id) {
        Order order = getOrderEntityById(id);
        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        return mapToResOrderDTO(order, items);
    }

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
        List<OrderLineItem> allItems = orderIds.isEmpty() ? new ArrayList<>() : orderLineItemRepository.findByOrderIdIn(orderIds);
        Map<Integer, List<OrderLineItem>> itemsByOrderId = allItems.stream().collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        List<ResOrderDTO> listRes = pageOrder.getContent().stream()
                .map(order -> mapToResOrderDTO(order, itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>())))
                .toList();
        rs.setResult(listRes);
        return rs;
    }

    @Transactional
    public ResOrderDTO updateOrder(Integer id, ReqCreateOrderDTO dto, String username) {
        User createdBy = getUserByUsername(username);
        Order order = getOrderEntityById(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể cập nhật đơn hàng ở trạng thái Đang xử lý");
        }

        Customer customer = getCustomerById(dto.getCustomerId());

        // 1. Revert old stock
        List<OrderLineItem> oldItems = orderLineItemRepository.findByOrderId(id);
        restoreProductStock(oldItems);
        orderLineItemRepository.deleteAll(oldItems);

        // 2. Set new basic info
        initOrderInfo(order, dto, customer, createdBy);

        // 3. Process new items
        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher = applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        // Deduct new stock
        deductProductStock(dto.getItems());

        Order savedOrder = orderRepository.save(order);
        orderLineItemRepository.saveAll(lineItems);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            processOrderCompletion(savedOrder, customer, appliedVoucher, dto.getPaymentMethod());
        }

        return mapToResOrderDTO(savedOrder, lineItems);
    }

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

        // Nếu đơn hàng đã hoàn tất trước đó thì mới cần hoàn voucher và hoàn điểm
        if (previousStatus == OrderStatus.COMPLETED) {
            revertOrderCompletion(savedOrder, customer);
        }

        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        restoreProductStock(items);

        return mapToResOrderDTO(savedOrder, items);
    }

    @Transactional
    public ResOrderDTO updatePrintStatus(Integer id, boolean status) {
        Order order = getOrderEntityById(id);
        order.setPrinted(status);
        orderRepository.save(order);
        return mapToResOrderDTO(order, orderLineItemRepository.findByOrderId(id));
    }

    // --- Helper Methods ---

    private User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        return user;
    }

    private Customer getCustomerById(Integer id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Khách hàng ID " + id + " không tồn tại"));
    }

    private Order getOrderEntityById(Integer id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
    }

    private void initOrderInfo(Order order, ReqCreateOrderDTO dto, Customer customer, User createdBy) {
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getFullName());
        order.setCreatedBy(createdBy.getId());
        order.setCreatedByUsername(createdBy.getUsername());
        order.setNote(dto.getNote());
        order.setPaidAmount(dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO);
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : OrderStatus.COMPLETED);
        if (order.getId() == null) order.setPrinted(false);
    }

    private BigDecimal buildOrderItems(List<ReqCreateOrderDTO.OrderItemDTO> itemsDto, Order order, List<OrderLineItem> lineItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : itemsDto) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm ID " + itemDto.getVariantId() + " không tồn tại"));

            BigDecimal unitPrice = variant.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderLineItem lineItem = new OrderLineItem();
            lineItem.setVariantId(variant.getId());

            String fullName = variant.getProduct().getName();
            List<String> options = new ArrayList<>();
            if (variant.getOption1Value() != null) options.add(variant.getOption1Value().getValue());
            if (variant.getOption2Value() != null) options.add(variant.getOption2Value().getValue());
            if (variant.getOption3Value() != null) options.add(variant.getOption3Value().getValue());
            if (!options.isEmpty()) fullName += " (" + String.join(" - ", options) + ")";

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

    private com.sapo.mock.clothing.entity.CustomerVoucher applyVoucher(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal) {
        if (dto.getVoucherCode() == null || dto.getVoucherCode().trim().isEmpty()) {
            order.setVoucherCode(null);
            order.setDiscountFromVoucher(BigDecimal.ZERO);
            return null;
        }

        com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher = customerVoucherRepository
                .findUnusedVoucherByCustomerAndCode(customer.getId(), dto.getVoucherCode().trim())
                .orElseThrow(() -> new BadRequestException("Mã voucher không hợp lệ, không tồn tại hoặc đã được sử dụng"));
        
        Voucher voucher = appliedVoucher.getVoucher();
        if (voucher.getStatus() != com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum.ACTIVE) {
            throw new BadRequestException("Voucher này đã bị khóa");
        }
        if (appliedVoucher.getExpiredAt().isBefore(Instant.now())) {
            throw new BadRequestException("Voucher này đã hết hạn");
        }
        if (voucher.getMinOrderValue() != null && currentTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinOrderValue() + ") để dùng voucher này");
        }

        BigDecimal discount = voucher.getDiscountAmount().min(currentTotal);
        order.setVoucherCode(voucher.getCode());
        order.setDiscountFromVoucher(discount);
        return appliedVoucher;
    }

    private void applyPoints(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal) {
        if (dto.getPointsToUse() == null || dto.getPointsToUse() <= 0) {
            order.setPointsUsed(0);
            order.setDiscountFromPoints(BigDecimal.ZERO);
            return;
        }

        if (customer.getRewardPoints() < dto.getPointsToUse()) {
            throw new BadRequestException("Khách hàng không đủ điểm. Điểm hiện tại: " + customer.getRewardPoints());
        }
        
        BigDecimal discount = BigDecimal.valueOf(dto.getPointsToUse()).multiply(PointConstant.REDEEM_RATE).min(currentTotal);
        order.setPointsUsed(dto.getPointsToUse());
        order.setDiscountFromPoints(discount);
    }

    private void finalizeOrderAmounts(ReqCreateOrderDTO dto, Order order, BigDecimal totalAmount) {
        order.setTotalAmount(totalAmount);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            BigDecimal paid = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;
            if (paid.compareTo(totalAmount) < 0) {
                throw new BadRequestException("Số tiền khách đưa (" + paid + ") không đủ để thanh toán đơn hàng (" + totalAmount + ")");
            }
            order.setChangeAmount(paid.subtract(totalAmount));
            order.setPointsEarned(order.getCustomerId() == 1 ? 0 : totalAmount.divideToIntegralValue(PointConstant.EARN_RATE).intValue());
        } else {
            order.setChangeAmount(BigDecimal.ZERO);
            order.setPointsEarned(0);
        }
    }

    private void processOrderCompletion(Order savedOrder, Customer customer, com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher, String paymentMethod) {
        if (appliedVoucher != null) {
            appliedVoucher.setStatus(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.USED);
            appliedVoucher.setUsedAt(Instant.now());
            appliedVoucher.setOrderId(savedOrder.getId());
            customerVoucherRepository.save(appliedVoucher);
        }

        if (customer.getId() != 1) {
            if (savedOrder.getPointsUsed() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() - savedOrder.getPointsUsed());
                savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsUsed(), PointConstant.TYPE_REDEEM, "Sử dụng điểm cho đơn hàng " + savedOrder.getOrderNumber());
            }
            if (savedOrder.getPointsEarned() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() + savedOrder.getPointsEarned());
                savePointHistory(customer.getId(), savedOrder.getId(), savedOrder.getPointsEarned(), PointConstant.TYPE_EARN, "Tích điểm từ đơn hàng " + savedOrder.getOrderNumber());
            }
            customerRepository.save(customer);
        }

        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
        sendOrderNotifications(savedOrder, paymentMethod);
    }

    private void revertOrderCompletion(Order savedOrder, Customer customer) {
        customerVoucherRepository.findByOrderId(savedOrder.getId()).ifPresent(cv -> {
            cv.setStatus(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.UNUSED);
            cv.setUsedAt(null);
            cv.setOrderId(null);
            customerVoucherRepository.save(cv);
        });

        if (customer.getId() != 1) {
            if (savedOrder.getPointsUsed() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() + savedOrder.getPointsUsed());
                savePointHistory(customer.getId(), savedOrder.getId(), savedOrder.getPointsUsed(), PointConstant.TYPE_REFUND, "Hoàn điểm do hủy đơn hàng " + savedOrder.getOrderNumber());
            }
            if (savedOrder.getPointsEarned() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() - savedOrder.getPointsEarned());
                savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsEarned(), PointConstant.TYPE_REFUND, "Trừ điểm tích lũy do hủy đơn hàng " + savedOrder.getOrderNumber());
            }
            customerRepository.save(customer);
        }
    }

    private void savePointHistory(Integer customerId, Integer orderId, int pointsChange, String type, String description) {
        PointHistory ph = new PointHistory();
        ph.setCustomerId(customerId);
        ph.setOrderId(orderId);
        ph.setPointsChange(pointsChange);
        ph.setType(type);
        ph.setDescription(description);
        pointHistoryRepository.save(ph);
    }

    private void deductProductStock(List<ReqCreateOrderDTO.OrderItemDTO> items) {
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : items) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID " + itemDto.getVariantId()));

            if (variant.getQuantity() < itemDto.getQuantity()) {
                throw new BadRequestException("Sản phẩm ID " + itemDto.getVariantId() + " không đủ số lượng (Hiện có: " + variant.getQuantity() + ")");
            }

            variant.setQuantity(variant.getQuantity() - itemDto.getQuantity());
            ProductVariant savedVariant = productVariantRepository.save(variant);

            if (savedVariant.getQuantity() <= savedVariant.getLowStockThreshold()) {
                try {
                    Notification lowStockAlert = new Notification();
                    lowStockAlert.setTitle("Cảnh báo tồn kho chạm ngưỡng");
                    
                    String fullName = savedVariant.getProduct().getName();
                    List<String> options = new ArrayList<>();
                    if (savedVariant.getOption1Value() != null) options.add(savedVariant.getOption1Value().getValue());
                    if (savedVariant.getOption2Value() != null) options.add(savedVariant.getOption2Value().getValue());
                    if (savedVariant.getOption3Value() != null) options.add(savedVariant.getOption3Value().getValue());
                    if (!options.isEmpty()) fullName += " (" + String.join(" - ", options) + ")";

                    lowStockAlert.setMessage(String.format("Cảnh báo: Mặt hàng [%s] %s đã chạm ngưỡng tồn kho tối thiểu (Còn %d chiếc).", savedVariant.getSku(), fullName, savedVariant.getQuantity()));
                    lowStockAlert.setType("LOW_STOCK");
                    lowStockAlert.setTargetRole("ROLE_WH");
                    lowStockAlert.setMetadata(String.format("{\"variantId\":%d,\"sku\":\"%s\",\"quantity\":%d}", savedVariant.getId(), savedVariant.getSku(), savedVariant.getQuantity()));
                    notificationService.sendNotification(lowStockAlert);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi thông báo tồn kho: " + e.getMessage());
                }
            }
        }
    }

    private void restoreProductStock(List<OrderLineItem> items) {
        for (OrderLineItem item : items) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tồn kho của sản phẩm ID " + item.getVariantId()));
            variant.setQuantity(variant.getQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }
    }

    private void sendOrderNotifications(Order savedOrder, String paymentMethod) {
        try {
            String payMethod = "QR_SEPAY".equals(paymentMethod) ? "Chuyển khoản" : "Tiền mặt";
            String customerName = savedOrder.getCustomerName();
            if (customerName == null || customerName.trim().isEmpty() || customerName.contains("Khách lẻ")) customerName = "Khách lẻ";

            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###", new java.text.DecimalFormatSymbols(java.util.Locale.US));
            String amountFormatted = df.format(savedOrder.getTotalAmount()) + " VND";

            Notification createNotif = new Notification();
            createNotif.setTitle("Đơn hàng mới");
            createNotif.setMessage(String.format("Đơn hàng %s được mua bởi %s qua nguồn đơn POS", savedOrder.getOrderNumber(), customerName));
            createNotif.setType("ORDER_CREATED");
            createNotif.setTargetRole("ROLE_ADMIN");
            createNotif.setMetadata(String.format("{\"orderId\":%d,\"orderNumber\":\"%s\"}", savedOrder.getId(), savedOrder.getOrderNumber()));
            notificationService.sendNotification(createNotif);

            Notification paidNotif = new Notification();
            paidNotif.setTitle("Thanh toán thành công");
            paidNotif.setMessage(String.format("Đơn hàng %s được thanh toán %s thành công bằng phương thức %s", savedOrder.getOrderNumber(), amountFormatted, payMethod));
            paidNotif.setType("ORDER_PAID");
            paidNotif.setMetadata(String.format("{\"orderId\":%d,\"orderNumber\":\"%s\"}", savedOrder.getId(), savedOrder.getOrderNumber()));
            notificationService.sendNotification(paidNotif);
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo đơn hàng mới: " + e.getMessage());
        }
    }

    @Transactional
    public void completeOrderPayment(String orderNumber, BigDecimal paidAmount) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));

        if (order.getStatus() != OrderStatus.PENDING) {
            return; 
        }

        if (paidAmount.compareTo(order.getTotalAmount()) < 0) {
            sendPaymentFailureNotification(order, paidAmount);
            throw new BadRequestException("Số tiền thanh toán không đủ");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPaidAmount(paidAmount);
        order.setChangeAmount(paidAmount.subtract(order.getTotalAmount()));
        order.setPointsEarned(order.getCustomerId() == 1 ? 0 : order.getTotalAmount().divideToIntegralValue(PointConstant.EARN_RATE).intValue());
        
        Order savedOrder = orderRepository.save(order);

        Customer customer = getCustomerById(savedOrder.getCustomerId());
        
        com.sapo.mock.clothing.entity.CustomerVoucher appliedVoucher = null;
        if (savedOrder.getVoucherCode() != null && !savedOrder.getVoucherCode().trim().isEmpty()) {
            appliedVoucher = customerVoucherRepository
                    .findUnusedVoucherByCustomerAndCode(customer.getId(), savedOrder.getVoucherCode().trim())
                    .orElse(null);
        }

        processOrderCompletion(savedOrder, customer, appliedVoucher, "QR_SEPAY");
    }

    private void sendPaymentFailureNotification(Order order, BigDecimal paidAmount) {
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###", new java.text.DecimalFormatSymbols(java.util.Locale.US));
            String paidFormatted = df.format(paidAmount) + " VND";
            String totalFormatted = df.format(order.getTotalAmount()) + " VND";

            Notification failNotif = new Notification();
            failNotif.setTitle("Thanh toán thiếu tiền");
            failNotif.setMessage(String.format("Cảnh báo: Đơn hàng %s chuyển thiếu tiền! Khách chuyển %s, cần thanh toán %s.", 
                    order.getOrderNumber(), paidFormatted, totalFormatted));
            failNotif.setType("SYSTEM");
            failNotif.setMetadata(String.format("{\"orderNumber\":\"%s\",\"type\":\"PAYMENT_INSUFFICIENT\",\"paidAmount\":%s,\"totalAmount\":%s}", 
                    order.getOrderNumber(), paidAmount.toString(), order.getTotalAmount().toString()));
            notificationService.sendNotification(failNotif);
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo thanh toán thiếu: " + e.getMessage());
        }
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
