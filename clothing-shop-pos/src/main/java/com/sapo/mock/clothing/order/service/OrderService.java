//package com.sapo.mock.clothing.order.service;
//
//import com.sapo.mock.clothing.entity.*;
//import com.sapo.mock.clothing.exception.BadRequestException;
//import com.sapo.mock.clothing.exception.ResourceNotFoundException;
//import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
//import com.sapo.mock.clothing.order.dto.ResOrderDTO;
//import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
//import com.sapo.mock.clothing.order.repository.OrderRepository;
//import com.sapo.mock.clothing.product.repository.ProductRepository;
//import com.sapo.mock.clothing.user.repository.UserRepository;
//import com.sapo.mock.clothing.util.constant.InvoiceStatus;
//import com.sapo.mock.clothing.customer.repository.CustomerRepository;
//import com.sapo.mock.clothing.warehouse.repository.warehouseRepository;
//import com.sapo.mock.clothing.warehouse.repository.warehouseStockRepository;
//import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class OrderService {
//
//    private final OrderRepository orderRepository;
//    private final OrderLineItemRepository orderLineItemRepository;
//    private final ProductRepository productRepository;
//    private final UserRepository userRepository;
//    private final warehouseRepository warehouseRepository;
//    private final warehouseStockRepository warehouseStockRepository;
//    private final CustomerRepository customerRepository;
//
//    @Transactional
//    public ResOrderDTO createOrder(ReqCreateOrderDTO dto, String username) {
//
//        // Tìm các entity liên quan từ DB
//        User createdBy = userRepository.findByUsername(username);
//        if (createdBy == null) {
//            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
//        }
//
//        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
//                .orElseThrow(() -> new ResourceNotFoundException("Kho ID " + dto.getWarehouseId() + " không tồn tại"));
//
//        Customer customer = customerRepository.findById(dto.getCustomerId())
//                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng ID " + dto.getCustomerId() + " không tồn tại"));
//
//        // Tạo đối tượng Order mới
//        Order order = new Order();
//        order.setCustomerId(customer.getId());
//        order.setCustomerName(customer.getFullName());
//        order.setWarehouseId(warehouse.getId());
//        order.setWarehouseName(warehouse.getName());
//        order.setCreatedBy(createdBy.getId());
//        order.setCreatedByUsername(createdBy.getUsername());
//        order.setNote(dto.getNote());
//        order.setPaidAmount(dto.getPaidAmount());
//        order.setStatus(InvoiceStatus.COMPLETED);
//        order.setPrinted(false);
//
//        // Tự động sinh mã đơn hàng dạng HD-YYYYMMDD-NNN
//        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
//        long countToday = orderRepository.countByCreatedAtAfter(startOfDay);
//        String orderNumber = "HD-" + dateStr + "-" + String.format("%03d", countToday + 1);
//        order.setOrderNumber(orderNumber);
//
//        // Duyệt danh sách sản phẩm, tính tổng tiền và tạo OrderLineItem (Snapshot)
//        BigDecimal totalAmount = BigDecimal.ZERO;
//        List<OrderLineItem> lineItems = new ArrayList<>();
//
//        for (ReqCreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
//            Product product = productRepository.findById(itemDto.getVariantId())
//                    .orElseThrow(() -> new ResourceNotFoundException(
//                            "Sản phẩm ID " + itemDto.getVariantId() + " không tồn tại"));
//
//            BigDecimal unitPrice = product.getSalePrice();
//            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
//            totalAmount = totalAmount.add(subtotal);
//
//            // Snapshot thông tin sản phẩm tại thời điểm bán
//            OrderLineItem lineItem = new OrderLineItem();
//            lineItem.setProductId(product.getId());
//            lineItem.setProductName(product.getName());
//            lineItem.setProductSku(product.getSku());
//            lineItem.setQuantity(itemDto.getQuantity());
//            lineItem.setUnitPrice(unitPrice);
//            lineItem.setSubtotal(subtotal);
//            lineItem.setOrder(order);
//
//            lineItems.add(lineItem);
//        }
//
//        // Kiểm tra số tiền khách đưa có đủ không
//        if (dto.getPaidAmount().compareTo(totalAmount) < 0) {
//            throw new BadRequestException(
//                    "Số tiền khách đưa (" + dto.getPaidAmount() + ") không đủ để thanh toán đơn hàng (" + totalAmount + ")");
//        }
//
//        // Gán tổng tiền và tiền thừa trả lại khách
//        order.setTotalAmount(totalAmount);
//        order.setChangeAmount(dto.getPaidAmount().subtract(totalAmount));
//
//        // Kiểm tra và trừ kho trước khi chốt đơn
//        this.deductWarehouseStock(dto.getItems(), warehouse.getId());
//
//        // Lưu đơn hàng cha trước (để có ID), sau đó lưu các dòng chi tiết
//        Order savedOrder = orderRepository.save(order);
//        orderLineItemRepository.saveAll(lineItems);
//
//        return mapToResOrderDTO(savedOrder, lineItems);
//    }
//
//    public ResOrderDTO getOrderById(Integer id) {
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
//        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
//
//        return mapToResOrderDTO(order, items);
//    }
//
//    public ResultPaginationDTO getAllOrders(Pageable pageable) {
//        Page<Order> pageOrder = orderRepository.findAll(pageable);
//        ResultPaginationDTO rs = new ResultPaginationDTO();
//        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
//
//        meta.setPage(pageable.getPageNumber() + 1);
//        meta.setPageSize(pageable.getPageSize());
//        meta.setPages(pageOrder.getTotalPages());
//        meta.setTotal(pageOrder.getTotalElements());
//
//        rs.setMeta(meta);
//
//        List<Integer> orderIds = pageOrder.getContent().stream()
//                .map(Order::getId)
//                .toList();
//
//        // Tối ưu N+1: batch fetch toàn bộ line items của tất cả đơn hàng trong trang
//        List<OrderLineItem> allItems = orderIds.isEmpty() ? new ArrayList<>()
//                : orderLineItemRepository.findByOrderIdIn(orderIds);
//
//        Map<Integer, List<OrderLineItem>> itemsByOrderId = allItems.stream()
//                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
//
//        List<ResOrderDTO> listRes = pageOrder.getContent().stream()
//                .map(order -> {
//                    List<OrderLineItem> items = itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>());
//                    return mapToResOrderDTO(order, items);
//                })
//                .toList();
//
//        rs.setResult(listRes);
//        return rs;
//    }
//
//    @Transactional
//    public ResOrderDTO cancelOrder(Integer id) {
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
//
//        if (order.getStatus() == InvoiceStatus.CANCELLED) {
//            throw new BadRequestException("Đơn hàng này đã bị hủy trước đó");
//        }
//
//        // Cập nhật trạng thái
//        order.setStatus(InvoiceStatus.CANCELLED);
//        Order savedOrder = orderRepository.save(order);
//
//        // Hoàn trả tồn kho
//        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
//        for (OrderLineItem item : items) {
//            WarehouseStock stock = warehouseStockRepository
//                    .findByProductIdAndWarehouseId(item.getProductId(), order.getWarehouseId())
//                    .orElseThrow(() -> new ResourceNotFoundException(
//                            "Không tìm thấy thông tin tồn kho của sản phẩm ID " + item.getProductId()
//                                    + " trong kho " + order.getWarehouseId()));
//
//            stock.setQuantity(stock.getQuantity() + item.getQuantity());
//            warehouseStockRepository.save(stock);
//        }
//
//        return mapToResOrderDTO(savedOrder, items);
//    }
//
//    /**
//     * Đánh dấu trạng thái in hóa đơn nhiệt cho đơn hàng.
//     *
//     * @param id     ID đơn hàng
//     * @param status true = đã in, false = chưa in
//     */
//    @Transactional
//    public ResOrderDTO updatePrintStatus(Integer id, boolean status) {
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng ID " + id + " không tồn tại"));
//
//        order.setPrinted(status);
//        Order savedOrder = orderRepository.save(order);
//
//        List<OrderLineItem> items = orderLineItemRepository.findByOrderId(id);
//        return mapToResOrderDTO(savedOrder, items);
//    }
//
//    // ─── Private helpers ─────────────────────────────────────────────────────
//
//    private ResOrderDTO mapToResOrderDTO(Order savedOrder, List<OrderLineItem> lineItems) {
//        List<ResOrderDTO.ResOrderItemDTO> resItems = lineItems.stream()
//                .map(i -> ResOrderDTO.ResOrderItemDTO.builder()
//                        .id(i.getId())
//                        .productId(i.getProductId())
//                        .productName(i.getProductName())
//                        .productSku(i.getProductSku())
//                        .quantity(i.getQuantity())
//                        .unitPrice(i.getUnitPrice())
//                        .subtotal(i.getSubtotal())
//                        .build())
//                .toList();
//
//        return ResOrderDTO.builder()
//                .id(savedOrder.getId())
//                .orderNumber(savedOrder.getOrderNumber())
//                .customerId(savedOrder.getCustomerId())
//                .customerName(savedOrder.getCustomerName())
//                .warehouseId(savedOrder.getWarehouseId())
//                .warehouseName(savedOrder.getWarehouseName())
//                .createdById(savedOrder.getCreatedBy())
//                .createdByUsername(savedOrder.getCreatedByUsername())
//                .totalAmount(savedOrder.getTotalAmount())
//                .paidAmount(savedOrder.getPaidAmount())
//                .changeAmount(savedOrder.getChangeAmount())
//                .status(savedOrder.getStatus())
//                .isPrinted(savedOrder.isPrinted())
//                .note(savedOrder.getNote())
//                .createdAt(savedOrder.getCreatedAt())
//                .updatedAt(savedOrder.getUpdatedAt())
//                .items(resItems)
//                .build();
//    }
//
//    private void deductWarehouseStock(List<ReqCreateOrderDTO.OrderItemDTO> items, Integer warehouseId) {
//        for (ReqCreateOrderDTO.OrderItemDTO itemDto : items) {
//            WarehouseStock stock = warehouseStockRepository
//                    .findByProductIdAndWarehouseId(itemDto.getVariantId(), warehouseId)
//                    .orElseThrow(() -> new ResourceNotFoundException(
//                            "Không tìm thấy thông tin tồn kho của sản phẩm ID " + itemDto.getVariantId()
//                                    + " trong kho " + warehouseId));
//
//            if (stock.getQuantity() < itemDto.getQuantity()) {
//                throw new BadRequestException("Sản phẩm ID " + itemDto.getVariantId()
//                        + " không đủ số lượng trong kho (Hiện có: " + stock.getQuantity() + ")");
//            }
//
//            // Thực hiện trừ kho vật lý
//            stock.setQuantity(stock.getQuantity() - itemDto.getQuantity());
//            warehouseStockRepository.save(stock);
//        }
//    }
//}
