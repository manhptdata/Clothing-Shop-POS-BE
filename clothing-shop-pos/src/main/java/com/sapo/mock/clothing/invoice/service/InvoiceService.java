package com.sapo.mock.clothing.invoice.service;

import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.invoice.dto.ReqCreateInvoiceDTO;
import com.sapo.mock.clothing.invoice.repository.InvoiceItemRepository;
import com.sapo.mock.clothing.invoice.repository.InvoiceRepository;
import com.sapo.mock.clothing.product.repository.ProductRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.InvoiceStatus;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.warehouse.repository.warehouseRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.sapo.mock.clothing.invoice.dto.ResInvoiceDTO;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final warehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public ResInvoiceDTO createInvoice(ReqCreateInvoiceDTO dto, String username) {

        // Tìm các entity liên quan từ DB
        User createdBy = userRepository.findByUsername(username);
        if (createdBy == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + username);
        }

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Kho ID " + dto.getWarehouseId() + " không tồn tại"));

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Khách hàng ID " + dto.getCustomerId() + " không tồn tại"));

        // Tạo đối tượng Invoice mới
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setCustomerName(customer.getFullName());
        invoice.setWarehouseId(warehouse.getId());
        invoice.setWarehouseName(warehouse.getName());
        invoice.setCreatedBy(createdBy.getId());
        invoice.setCreatedByUsername(createdBy.getUsername());
        invoice.setNote(dto.getNote());
        invoice.setPaidAmount(dto.getPaidAmount());
        invoice.setStatus(InvoiceStatus.COMPLETED);

        // Tự động sinh mã hóa đơn dạng HD-YYYYMMDD-NNN
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long countToday = invoiceRepository.countByCreatedAtAfter(startOfDay);
        String invoiceCode = "HD-" + dateStr + "-" + String.format("%03d", countToday + 1);
        invoice.setCode(invoiceCode);

        // Duyệt danh sách sản phẩm, tính tổng tiền và tạo InvoiceItem
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (ReqCreateInvoiceDTO.InvoiceItemDTO itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm ID " + itemDto.getProductId() + " không tồn tại"));

            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            // Snapshot thông tin sản phẩm tại thời điểm bán
            InvoiceItem item = new InvoiceItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            item.setInvoice(invoice);

            invoiceItems.add(item);
        }

        // Kiểm tra số tiền khách đưa có đủ không
        if (dto.getPaidAmount().compareTo(totalAmount) < 0) {
            throw new BadRequestException(
                    "Số tiền khách đưa (" + dto.getPaidAmount() + ") không đủ để thanh toán đơn hàng (" + totalAmount
                            + ")");
        }

        // Gán tổng tiền và tiền thừa trả lại khách
        invoice.setTotalAmount(totalAmount);
        invoice.setChangeAmount(dto.getPaidAmount().subtract(totalAmount));

        // Lưu hóa đơn cha trước (để có ID), sau đó lưu các dòng chi tiết
        Invoice savedInvoice = invoiceRepository.save(invoice);
        invoiceItemRepository.saveAll(invoiceItems);

        // Map sang ResInvoiceDTO để trả về thực tế
        List<ResInvoiceDTO.ResInvoiceItemDTO> resItems = invoiceItems.stream()
                .map(i -> ResInvoiceDTO.ResInvoiceItemDTO.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .productSku(i.getProductSku())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return ResInvoiceDTO.builder()
                .id(savedInvoice.getId())
                .code(savedInvoice.getCode())
                .customerId(savedInvoice.getCustomerId())
                .customerName(savedInvoice.getCustomerName())
                .warehouseId(savedInvoice.getWarehouseId())
                .warehouseName(savedInvoice.getWarehouseName())
                .createdById(savedInvoice.getCreatedBy())
                .createdByUsername(savedInvoice.getCreatedByUsername())
                .totalAmount(savedInvoice.getTotalAmount())
                .paidAmount(savedInvoice.getPaidAmount())
                .changeAmount(savedInvoice.getChangeAmount())
                .status(savedInvoice.getStatus())
                .note(savedInvoice.getNote())
                .createdAt(savedInvoice.getCreatedAt())
                .updatedAt(savedInvoice.getUpdatedAt())
                .items(resItems)
                .build();
    }
}