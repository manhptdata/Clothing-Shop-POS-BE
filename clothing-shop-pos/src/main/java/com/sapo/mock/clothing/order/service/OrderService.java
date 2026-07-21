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

        // Chặn giam kho: Giới hạn số đơn PENDING tối đa per customer (Cài đặt động
        // trong SystemSetting)
        if (customer != null && customer.getId() != null && customer.getId() != 1) {
            int maxPending = systemSettingService.getMaxPendingOrdersLimit();
            long currentPending = orderRepository.countByCustomerIdAndStatus(customer.getId(), OrderStatus.PENDING);
            if (currentPending >= maxPending) {
                throw new BadRequestException("Khách hàng đang có " + currentPending
                        + " đơn hàng chưa thanh toán (Tối đa cho phép là " + maxPending
                        + " đơn). Vui lòng hoàn tất hoặc hủy các đơn cũ trước khi tạo đơn mới.");
            }
        }

        // Bug #5 fix: Không cho tạo đơn hàng rỗng
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }
        mergeDuplicateItems(dto);

        Order order = new Order();
        initOrderInfo(order, dto, customer, createdBy);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        order.setOrderNumber("HD-" + dateStr + "-" + timeStr);

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        CustomerVoucher appliedVoucher = orderLoyaltyService.applyVoucher(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromVoucher());

        orderLoyaltyService.applyPoints(dto, order, customer, totalAmount);
        totalAmount = totalAmount.subtract(order.getDiscountFromPoints());

        finalizeOrderAmounts(dto, order, totalAmount);

        Order savedOrder = orderRepository.save(order);
        orderLineItemRepository.saveAll(lineItems);

        // Truyền orderNumber và savedOrder.getId() thật vào deductProductStock để ghi
        // StockLog
        orderInventoryService.deductProductStock(dto.getItems(), savedOrder.getId(), savedOrder.getOrderNumber());

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
                .orElseThrow(() -> new com.sapo.mock.clothing.exception.ResourceNotFoundException(
                        "Đơn hàng không tồn tại: " + orderNumber));
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
        Order order = orderRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Không thể cập nhật: Đơn hàng đã được thanh toán thành công trong lúc bạn đang thao tác.");
        }

        Customer customer = getCustomerById(dto.getCustomerId());
        Customer oldCustomer = getCustomerById(order.getCustomerId());
        boolean isCustomerChanged = !oldCustomer.getId().equals(customer.getId());

        if (isCustomerChanged && customer.getId() != 1) {
            int maxPending = systemSettingService.getMaxPendingOrdersLimit();
            long currentPending = orderRepository.countByCustomerIdAndStatus(customer.getId(), OrderStatus.PENDING);
            if (currentPending >= maxPending) {
                throw new BadRequestException("Khách hàng được chọn đang có " + currentPending
                        + " đơn hàng chưa thanh toán (Tối đa cho phép là " + maxPending
                        + " đơn). Vui lòng hoàn tất hoặc hủy các đơn cũ trước khi chuyển đơn cho khách hàng này.");
            }
        }

        List<OrderLineItem> oldItems = orderLineItemRepository.findByOrderId(id);
        orderInventoryService.restoreProductStock(oldItems, id, order.getOrderNumber());
        orderLineItemRepository.deleteAll(oldItems);

        String oldVoucherCode = order.getVoucherCode();
        String newVoucherCode = dto.getVoucherCode();
        boolean isSamePublicVoucher = !isCustomerChanged && oldVoucherCode != null && !oldVoucherCode.isBlank()
                && oldVoucherCode.trim().equalsIgnoreCase(newVoucherCode != null ? newVoucherCode.trim() : "");

        // Hoàn trả Loyalty (Điểm & Voucher) của đơn PENDING cũ trước khi tính toán áp dụng lại
        if (isCustomerChanged) {
            orderLoyaltyService.revertLoyaltyOnCancel(order, oldCustomer);
        } else {
            orderLoyaltyService.revertLoyaltyOnUpdate(order, oldCustomer, newVoucherCode);
        }

        // Bug #5 fix: Không cho cập nhật đơn hàng thành rỗng
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }
        mergeDuplicateItems(dto);

        initOrderInfo(order, dto, customer, createdBy);

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = buildOrderItems(dto.getItems(), order, lineItems);

        CustomerVoucher appliedVoucher = orderLoyaltyService.applyVoucher(dto, order, customer, totalAmount,
                isSamePublicVoucher);
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
        } else if (order.getStatus() == OrderStatus.PENDING) {
            if (appliedVoucher != null) {
                orderLoyaltyService.reserveVoucher(appliedVoucher, savedOrder.getId());
            }
            orderLoyaltyService.reservePoints(savedOrder, customer);
        }

        return mapToResOrderDTO(savedOrder, lineItems);
    }

    // Hủy đơn hàng
    @Transactional
    public ResOrderDTO cancelOrder(Integer id, ReqCancelOrderDTO dto, String username) {
        Order order = orderRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));

        if (systemSettingService.isCancelApprovalRequired()) {
            if (dto.getApprovalPin() == null || dto.getApprovalPin().isEmpty()) {
                throw new BadRequestException("Cần có mã PIN quản lý để duyệt hủy đơn hàng.");
            }

            // Kiểm tra PIN
            List<User> approvers = userRepository.findAll().stream()
                    .filter(u -> "ROLE_ADMIN".equals(u.getRole().getName())
                            || "ROLE_MANAGER".equals(u.getRole().getName()))
                    .filter(u -> u.getSecurityPin() != null && u.getSecurityPin().equals(dto.getApprovalPin()))
                    .toList();

            if (approvers.isEmpty()) {
                throw new BadRequestException("Mã PIN không chính xác hoặc người duyệt không có quyền.");
            }
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng này đã bị hủy trước đó");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đơn hàng vì đơn hàng đã thanh toán/hoàn thành.");
        }
        if (order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.PARTIALLY_RETURNED) {
            throw new BadRequestException("Không thể hủy đơn hàng đã có trả hàng. Vui lòng xử lý qua phiếu trả hàng.");
        }
        if (paymentLogRepository.existsByOrderNumberAndStatus(order.getOrderNumber(), "INSUFFICIENT")) {
            throw new BadRequestException("Đơn hàng đang có khoản chuyển thiếu tiền chưa xử lý. Vui lòng sang trang Lịch sử thanh toán thực hiện hoàn tiền trước khi hủy đơn.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(dto.getReason());
        order.setPaidAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        Customer customer = getCustomerById(order.getCustomerId());

        if (previousStatus == OrderStatus.PENDING) {
            orderLoyaltyService.revertLoyaltyOnCancel(savedOrder, customer);
        }

        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
        orderInventoryService.restoreProductStock(items, id, order.getOrderNumber());

        return mapToResOrderDTO(savedOrder, items);
    }

    // Đánh dấu đơn hàng đã in
    @Transactional
    public ResOrderDTO updatePrintStatus(Integer id, boolean status) {
        Order order = orderRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
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
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng ID " + id + " không tồn tại"));
        if (id != 1 && customer.getStatus() != com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE) {
            throw new BadRequestException("Tài khoản khách hàng đã bị khóa hoặc ngừng hoạt động.");
        }
        return customer;
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

        PaymentMethod pm = dto.getPaymentMethod() != null ? PaymentMethod.valueOf(dto.getPaymentMethod())
                : PaymentMethod.CASH;
        order.setPaymentMethod(pm);

        if (pm == PaymentMethod.QR_SEPAY) {
            order.setStatus(OrderStatus.PENDING);
            if (order.getId() == null) {
                order.setPaidAmount(BigDecimal.ZERO);
            }
            // Nếu update (getId() != null), giữ nguyên paidAmount cũ để không mất số tiền khách đã chuyển khoản 1 phần
        } else {
            order.setPaidAmount(dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO);
            OrderStatus requestedStatus = dto.getStatus();
            if (requestedStatus != null && requestedStatus != OrderStatus.COMPLETED && requestedStatus != OrderStatus.PENDING) {
                throw new BadRequestException("Trạng thái đơn hàng không hợp lệ. Chỉ được phép tạo/cập nhật ở trạng thái PENDING hoặc COMPLETED.");
            }
            order.setStatus(requestedStatus != null ? requestedStatus : OrderStatus.COMPLETED);
        }

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
            if (order.getPaymentMethod() == PaymentMethod.CASH) {
                order.setChangeAmount(paid.subtract(totalAmount));
            } else {
                order.setChangeAmount(BigDecimal.ZERO);
            }
            order.setPointsEarned(order.getCustomerId() == 1 ? 0
                    : totalAmount.divideToIntegralValue(PointConstant.EARN_RATE).intValue());
        } else {
            order.setChangeAmount(BigDecimal.ZERO);
            order.setPointsEarned(0);
        }
    }

    // Hoàn thành thanh toán đơn hàng bằng QR_SEPAY
    @Transactional
    public String completeOrderPayment(String orderNumber, BigDecimal paidAmount) {
        Order order = orderRepository.findByOrderNumberWithPessimisticLock(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderNumber));

        if (order.getStatus() != OrderStatus.PENDING) {
            orderNotificationHelper.sendDuplicatePaymentNotification(order, paidAmount);
            throw new com.sapo.mock.clothing.exception.DuplicatePaymentException(
                    "Đơn hàng đã được thanh toán hoặc không ở trạng thái chờ thanh toán.");
        }

        BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal totalPaid = currentPaid.add(paidAmount);
        order.setPaidAmount(totalPaid);
        order.setPaymentMethod(PaymentMethod.QR_SEPAY);

        if (totalPaid.compareTo(order.getTotalAmount()) < 0) {
            orderNotificationHelper.sendPaymentFailureNotification(order, paidAmount);
            orderRepository.save(order);
            return "INSUFFICIENT";
        }

        order.setStatus(OrderStatus.COMPLETED);
        BigDecimal overpaid = totalPaid.subtract(order.getTotalAmount());
        // Tiền thối chỉ áp dụng cho TIỀN MẶT. Khóa changeAmount = 0 cho QR_SEPAY để
        // tránh thất thoát tiền mặt.
        order.setChangeAmount(BigDecimal.ZERO);

        if (overpaid.compareTo(BigDecimal.ZERO) > 0) {
            orderNotificationHelper.sendOverpaymentNotification(order, overpaid);
        }
        order.setPointsEarned(order.getCustomerId() == 1 ? 0
                : order.getTotalAmount().divideToIntegralValue(PointConstant.EARN_RATE).intValue());

        Order savedOrder = orderRepository.save(order);
        Customer customer = getCustomerById(savedOrder.getCustomerId());

        CustomerVoucher appliedVoucher = orderLoyaltyService.getAppliedVoucher(savedOrder.getId());

        orderLoyaltyService.processLoyaltyOnCompletion(savedOrder, customer, appliedVoucher);
        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getCustomerId(), savedOrder.getTotalAmount()));
        orderNotificationHelper.sendOrderNotifications(savedOrder, "QR_SEPAY");

        return overpaid.compareTo(BigDecimal.ZERO) > 0 ? "OVERPAID" : "SUCCESS";
    }

    // Hoàn thành thanh toán đơn hàng bằng ID (sử dụng cho luồng POS thủ công/online
    // tối giản)
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
        PaymentMethod finalMethod = paymentMethod != null ? PaymentMethod.valueOf(paymentMethod) : PaymentMethod.CASH;
        order.setPaymentMethod(finalMethod);
        order.setPaidAmount(amount);
        if (finalMethod == PaymentMethod.CASH) {
            order.setChangeAmount(amount.subtract(order.getTotalAmount()));
        } else {
            order.setChangeAmount(BigDecimal.ZERO);
        }
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
            paymentLog.setTransactionDate(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            paymentLog.setContent("Thanh toán QR được xác nhận thủ công cho đơn " + savedOrder.getOrderNumber());
            paymentLog.setStatus("SUCCESS");
            paymentLogRepository.save(paymentLog);
        }

        orderNotificationHelper.sendOrderNotifications(savedOrder, paymentMethod);

        return mapToResOrderDTO(savedOrder, orderLineItemRepository.findByOrderId(savedOrder.getId()));
    }

    private BigDecimal getVoucherMinOrderValue(Order order) {
        if (order.getVoucherCode() == null)
            return null;
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

    private void mergeDuplicateItems(ReqCreateOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return;
        }
        Map<Integer, Long> groupedItems = new java.util.LinkedHashMap<>();
        for (ReqCreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
            if (itemDto.getVariantId() != null && itemDto.getQuantity() != null) {
                long newQty = groupedItems.getOrDefault(itemDto.getVariantId(), 0L) + itemDto.getQuantity();
                if (newQty > Integer.MAX_VALUE) {
                    throw new BadRequestException("Tổng số lượng cho một sản phẩm đã vượt quá giới hạn hệ thống.");
                }
                groupedItems.put(itemDto.getVariantId(), newQty);
            }
        }
        List<ReqCreateOrderDTO.OrderItemDTO> mergedItems = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : groupedItems.entrySet()) {
            ReqCreateOrderDTO.OrderItemDTO mergedItem = new ReqCreateOrderDTO.OrderItemDTO();
            mergedItem.setVariantId(entry.getKey());
            mergedItem.setQuantity(entry.getValue().intValue());
            mergedItems.add(mergedItem);
        }
        dto.setItems(mergedItems);
    }
}
