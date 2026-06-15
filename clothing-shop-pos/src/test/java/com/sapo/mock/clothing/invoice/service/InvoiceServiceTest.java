package com.sapo.mock.clothing.invoice.service;

import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.invoice.dto.ReqCreateInvoiceDTO;
import com.sapo.mock.clothing.invoice.dto.ResInvoiceDTO;
import com.sapo.mock.clothing.invoice.repository.InvoiceItemRepository;
import com.sapo.mock.clothing.invoice.repository.InvoiceRepository;
import com.sapo.mock.clothing.product.repository.ProductRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.warehouse.repository.warehouseRepository;
import com.sapo.mock.clothing.util.constant.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.warehouse.repository.warehouseStockRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private warehouseRepository warehouseRepository;
    @Mock
    private warehouseStockRepository warehouseStockRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private User mockUser;
    private Warehouse mockWarehouse;
    private Customer mockCustomer;
    private Product mockProduct;
    private ReqCreateInvoiceDTO mockReqDto;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        mockWarehouse = new Warehouse();
        mockWarehouse.setId(1);
        mockWarehouse.setName("Kho Trung Tâm");

        mockCustomer = new Customer();
        mockCustomer.setId(1);
        mockCustomer.setFullName("Nguyễn Văn A");

        mockProduct = new Product();
        mockProduct.setId(1);
        mockProduct.setName("Áo thun");
        mockProduct.setSku("AT-01");
        mockProduct.setSalePrice(new BigDecimal("100000"));

        mockReqDto = new ReqCreateInvoiceDTO();
        mockReqDto.setCustomerId(1);
        mockReqDto.setWarehouseId(1);
        mockReqDto.setNote("Test invoice");
        mockReqDto.setPaidAmount(new BigDecimal("200000"));

        ReqCreateInvoiceDTO.InvoiceItemDTO itemDto = new ReqCreateInvoiceDTO.InvoiceItemDTO();
        itemDto.setProductId(1);
        itemDto.setQuantity(2);

        mockReqDto.setItems(Collections.singletonList(itemDto));
    }

    @Test
    void createInvoice_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1)).thenReturn(Optional.of(mockCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(mockProduct));

        when(invoiceRepository.countByCreatedAtAfter(any())).thenReturn(0L);

        Invoice savedInvoice = new Invoice();
        savedInvoice.setId(100);
        savedInvoice.setCode("HD-20230101-001");
        savedInvoice.setCustomerId(1);
        savedInvoice.setCustomerName("Nguyễn Văn A");
        savedInvoice.setWarehouseId(1);
        savedInvoice.setWarehouseName("Kho Trung Tâm");
        savedInvoice.setCreatedBy(1);
        savedInvoice.setCreatedByUsername("testuser");
        savedInvoice.setTotalAmount(new BigDecimal("200000"));
        savedInvoice.setPaidAmount(new BigDecimal("200000"));
        savedInvoice.setChangeAmount(BigDecimal.ZERO);
        savedInvoice.setStatus(InvoiceStatus.COMPLETED);

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        // Act
        ResInvoiceDTO result = invoiceService.createInvoice(mockReqDto, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals("Nguyễn Văn A", result.getCustomerName());
        assertEquals("testuser", result.getCreatedByUsername());
        assertEquals(new BigDecimal("200000"), result.getTotalAmount());

        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(invoiceItemRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createInvoice_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            invoiceService.createInvoice(mockReqDto, "unknown");
        });
    }

    @Test
    void createInvoice_InsufficientPaidAmount_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1)).thenReturn(Optional.of(mockCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(mockProduct));

        // Khách đưa 50k, nhưng tổng tiền là 200k (100k x 2)
        mockReqDto.setPaidAmount(new BigDecimal("50000"));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            invoiceService.createInvoice(mockReqDto, "testuser");
        });
    }

    @Test
    void getInvoiceById_Success() {
        // Arrange
        Invoice invoice = new Invoice();
        invoice.setId(100);
        invoice.setCode("HD-001");
        invoice.setTotalAmount(new BigDecimal("200000"));

        InvoiceItem item = new InvoiceItem();
        item.setId(1);
        item.setProductId(1);
        item.setQuantity(2);

        when(invoiceRepository.findById(100)).thenReturn(Optional.of(invoice));
        when(invoiceItemRepository.findByInvoiceId(100)).thenReturn(Collections.singletonList(item));

        // Act
        ResInvoiceDTO result = invoiceService.getInvoiceById(100);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getQuantity());
    }

    @Test
    void getInvoiceById_NotFound_ThrowsException() {
        when(invoiceRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            invoiceService.getInvoiceById(999);
        });
    }

    @Test
    void getAllInvoices_Success() {
        // Arrange
        Invoice invoice1 = new Invoice();
        invoice1.setId(100);
        
        Invoice invoice2 = new Invoice();
        invoice2.setId(101);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Invoice> page = new PageImpl<>(Arrays.asList(invoice1, invoice2), pageable, 2);

        when(invoiceRepository.findAll(pageable)).thenReturn(page);
        when(invoiceItemRepository.findByInvoiceId(anyInt())).thenReturn(Collections.emptyList());

        // Act
        ResultPaginationDTO result = invoiceService.getAllInvoices(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMeta().getPage());
        assertEquals(5, result.getMeta().getPageSize());
        assertEquals(2, result.getMeta().getTotal());
        
        List<ResInvoiceDTO> resList = (List<ResInvoiceDTO>) result.getResult();
        assertEquals(2, resList.size());
    }

    @Test
    void cancelInvoice_Success() {
        // Arrange
        Invoice invoice = new Invoice();
        invoice.setId(100);
        invoice.setWarehouseId(1);
        invoice.setStatus(InvoiceStatus.COMPLETED);

        InvoiceItem item = new InvoiceItem();
        item.setProductId(1);
        item.setQuantity(5);

        WarehouseStock stock = new WarehouseStock();
        stock.setId(1);
        stock.setQuantity(10); // current stock

        when(invoiceRepository.findById(100)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceItemRepository.findByInvoiceId(100)).thenReturn(Collections.singletonList(item));
        when(warehouseStockRepository.findByProductIdAndWarehouseId(1, 1)).thenReturn(Optional.of(stock));

        // Act
        ResInvoiceDTO result = invoiceService.cancelInvoice(100);

        // Assert
        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        assertEquals(15, stock.getQuantity()); // 10 + 5 returned
        verify(warehouseStockRepository, times(1)).save(stock);
    }

    @Test
    void cancelInvoice_AlreadyCancelled_ThrowsException() {
        Invoice invoice = new Invoice();
        invoice.setId(100);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(100)).thenReturn(Optional.of(invoice));

        assertThrows(BadRequestException.class, () -> {
            invoiceService.cancelInvoice(100);
        });
    }
}
