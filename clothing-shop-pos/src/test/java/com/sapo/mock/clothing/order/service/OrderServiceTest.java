package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
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
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderLineItemRepository orderLineItemRepository;
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
    private OrderService orderService;

    private User mockUser;
    private Warehouse mockWarehouse;
    private Customer mockCustomer;
    private Product mockProduct;
    private ReqCreateOrderDTO mockReqDto;

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

        mockReqDto = new ReqCreateOrderDTO();
        mockReqDto.setCustomerId(1);
        mockReqDto.setWarehouseId(1);
        mockReqDto.setNote("Test order");
        mockReqDto.setPaidAmount(new BigDecimal("200000"));

        ReqCreateOrderDTO.OrderItemDTO itemDto = new ReqCreateOrderDTO.OrderItemDTO();
        itemDto.setVariantId(1);
        itemDto.setQuantity(2);

        mockReqDto.setItems(Collections.singletonList(itemDto));
    }

    @Test
    void createOrder_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1)).thenReturn(Optional.of(mockCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(mockProduct));

        when(orderRepository.countByCreatedAtAfter(any())).thenReturn(0L);

        WarehouseStock stock = new WarehouseStock();
        stock.setId(1);
        stock.setQuantity(50);
        when(warehouseStockRepository.findByProductIdAndWarehouseId(1, 1)).thenReturn(Optional.of(stock));

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setOrderNumber("HD-20230101-001");
        savedOrder.setCustomerId(1);
        savedOrder.setCustomerName("Nguyễn Văn A");
        savedOrder.setWarehouseId(1);
        savedOrder.setWarehouseName("Kho Trung Tâm");
        savedOrder.setCreatedBy(1);
        savedOrder.setCreatedByUsername("testuser");
        savedOrder.setTotalAmount(new BigDecimal("200000"));
        savedOrder.setPaidAmount(new BigDecimal("200000"));
        savedOrder.setChangeAmount(BigDecimal.ZERO);
        savedOrder.setStatus(InvoiceStatus.COMPLETED);
        savedOrder.setPrinted(false);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        ResOrderDTO result = orderService.createOrder(mockReqDto, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals("Nguyễn Văn A", result.getCustomerName());
        assertEquals("testuser", result.getCreatedByUsername());
        assertEquals(new BigDecimal("200000"), result.getTotalAmount());
        assertFalse(result.isPrinted());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderLineItemRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createOrder_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder(mockReqDto, "unknown");
        });
    }

    @Test
    void createOrder_InsufficientPaidAmount_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1)).thenReturn(Optional.of(mockCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(mockProduct));

        // Khách đưa 50k, nhưng tổng tiền là 200k (100k x 2)
        mockReqDto.setPaidAmount(new BigDecimal("50000"));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            orderService.createOrder(mockReqDto, "testuser");
        });
    }

    @Test
    void getOrderById_Success() {
        // Arrange
        Order order = new Order();
        order.setId(100);
        order.setOrderNumber("HD-001");
        order.setTotalAmount(new BigDecimal("200000"));
        order.setPrinted(true);

        OrderLineItem item = new OrderLineItem();
        item.setId(1);
        item.setProductId(1);
        item.setQuantity(2);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(item));

        // Act
        ResOrderDTO result = orderService.getOrderById(100);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getQuantity());
        assertTrue(result.isPrinted());
    }

    @Test
    void getOrderById_NotFound_ThrowsException() {
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderById(999);
        });
    }

    @Test
    void getAllOrders_Success() {
        // Arrange
        Order order1 = new Order();
        order1.setId(100);
        
        Order order2 = new Order();
        order2.setId(101);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Order> page = new PageImpl<>(Arrays.asList(order1, order2), pageable, 2);

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderLineItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        // Act
        ResultPaginationDTO result = orderService.getAllOrders(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMeta().getPage());
        assertEquals(5, result.getMeta().getPageSize());
        assertEquals(2, result.getMeta().getTotal());
        
        List<ResOrderDTO> resList = (List<ResOrderDTO>) result.getResult();
        assertEquals(2, resList.size());
    }

    @Test
    void cancelOrder_Success() {
        // Arrange
        Order order = new Order();
        order.setId(100);
        order.setWarehouseId(1);
        order.setStatus(InvoiceStatus.COMPLETED);

        OrderLineItem item = new OrderLineItem();
        item.setProductId(1);
        item.setQuantity(5);

        WarehouseStock stock = new WarehouseStock();
        stock.setId(1);
        stock.setQuantity(10); // current stock

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(item));
        when(warehouseStockRepository.findByProductIdAndWarehouseId(1, 1)).thenReturn(Optional.of(stock));

        // Act
        ResOrderDTO result = orderService.cancelOrder(100);

        // Assert
        assertEquals(InvoiceStatus.CANCELLED, order.getStatus());
        assertEquals(15, stock.getQuantity()); // 10 + 5 returned
        verify(warehouseStockRepository, times(1)).save(stock);
    }

    @Test
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        Order order = new Order();
        order.setId(100);
        order.setStatus(InvoiceStatus.CANCELLED);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> {
            orderService.cancelOrder(100);
        });
    }

    @Test
    void updatePrintStatus_Success() {
        // Arrange
        Order order = new Order();
        order.setId(100);
        order.setPrinted(false);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.emptyList());

        // Act
        ResOrderDTO result = orderService.updatePrintStatus(100, true);

        // Assert
        assertTrue(order.isPrinted());
        assertTrue(result.isPrinted());
        verify(orderRepository, times(1)).save(order);
    }
}
