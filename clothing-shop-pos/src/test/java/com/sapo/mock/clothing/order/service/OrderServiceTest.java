package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import com.sapo.mock.clothing.util.constant.OrderStatus;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//hello just test cicd

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderLineItemRepository orderLineItemRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PointHistoryRepository pointHistoryRepository;
    @Mock
    private com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User mockUser;
    private Customer mockCustomer;
    private Product mockProduct;
    private ProductVariant mockVariant;
    private ReqCreateOrderDTO mockReqDto;
    private Voucher mockVoucher;
    private CustomerVoucher mockCustomerVoucher;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        mockCustomer = new Customer();
        mockCustomer.setId(2);
        mockCustomer.setFullName("Nguyễn Văn A");
        mockCustomer.setRewardPoints(1000);

        mockProduct = new Product();
        mockProduct.setId(1);
        mockProduct.setName("Áo thun");

        mockVariant = new ProductVariant();
        mockVariant.setId(10);
        mockVariant.setSku("AT-01-RED-M");
        mockVariant.setSalePrice(new BigDecimal("100000"));
        mockVariant.setProduct(mockProduct);
        mockVariant.setQuantity(50); // initial stock

        mockReqDto = new ReqCreateOrderDTO();
        mockReqDto.setCustomerId(2);
        mockReqDto.setNote("Test order");
        mockReqDto.setPaidAmount(new BigDecimal("200000"));

        ReqCreateOrderDTO.OrderItemDTO itemDto = new ReqCreateOrderDTO.OrderItemDTO();
        itemDto.setVariantId(10);
        itemDto.setQuantity(2);

        mockReqDto.setItems(Collections.singletonList(itemDto));

        mockVoucher = new Voucher();
        mockVoucher.setId(1);
        mockVoucher.setCode("GIAM50K");
        mockVoucher.setDiscountAmount(new BigDecimal("50000"));
        mockVoucher.setMinOrderValue(new BigDecimal("100000"));
        mockVoucher.setStatus(com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum.ACTIVE);

        mockCustomerVoucher = new CustomerVoucher();
        mockCustomerVoucher.setId(1);
        mockCustomerVoucher.setCustomer(mockCustomer);
        mockCustomerVoucher.setVoucher(mockVoucher);
        mockCustomerVoucher.setStatus(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.UNUSED);
        mockCustomerVoucher.setExpiredAt(java.time.Instant.now().plusSeconds(86400)); // Hết hạn ngày mai
    }

    @Test
    void createOrder_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));

        when(orderRepository.countByCreatedAtAfter(any())).thenReturn(0L);

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setOrderNumber("HD-20230101-001");
        savedOrder.setCustomerId(2);
        savedOrder.setCustomerName("Nguyễn Văn A");
        savedOrder.setCreatedBy(1);
        savedOrder.setCreatedByUsername("testuser");
        savedOrder.setTotalAmount(new BigDecimal("200000"));
        savedOrder.setPaidAmount(new BigDecimal("200000"));
        savedOrder.setChangeAmount(BigDecimal.ZERO);
        savedOrder.setStatus(OrderStatus.COMPLETED);
        savedOrder.setPrinted(false);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        ResOrderDTO result = orderService.createOrder(mockReqDto, "testuser");

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals("Nguyễn Văn A", result.getCustomerName());
        assertEquals("testuser", result.getCreatedByUsername());
        assertEquals(new BigDecimal("200000"), result.getTotalAmount());
        assertFalse(result.isPrinted());
        assertEquals(48, mockVariant.getQuantity()); // 50 - 2
        assertEquals(1200, mockCustomer.getRewardPoints()); // 1000 + 200 points (200k / 1000)

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderLineItemRepository, times(1)).saveAll(anyList());
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
        verify(customerRepository, times(1)).save(mockCustomer);
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
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));

        mockReqDto.setPaidAmount(new BigDecimal("50000"));

        assertThrows(BadRequestException.class, () -> {
            orderService.createOrder(mockReqDto, "testuser");
        });
    }

    @Test
    void getOrderById_Success() {
        Order order = new Order();
        order.setId(100);
        order.setOrderNumber("HD-001");
        order.setTotalAmount(new BigDecimal("200000"));
        order.setPrinted(true);

        OrderLineItem item = new OrderLineItem();
        item.setId(1);
        item.setVariantId(10);
        item.setQuantity(2);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(item));

        ResOrderDTO result = orderService.getOrderById(100);

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
        Order order1 = new Order();
        order1.setId(100);

        Order order2 = new Order();
        order2.setId(101);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Order> page = new PageImpl<>(Arrays.asList(order1, order2), pageable, 2);

        when(orderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);
        when(orderLineItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        ResultPaginationDTO result = orderService.getAllOrders(pageable, null, null);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getPage());
        assertEquals(5, result.getMeta().getPageSize());
        assertEquals(2, result.getMeta().getTotal());

        List<ResOrderDTO> resList = (List<ResOrderDTO>) result.getResult();
        assertEquals(2, resList.size());
    }

    @Test
    void cancelOrder_Success() {
        Order order = new Order();
        order.setId(100);
        order.setCustomerId(2);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPointsEarned(200);

        OrderLineItem item = new OrderLineItem();
        item.setVariantId(10); // Matches mockVariant.getId()
        item.setQuantity(5);

        mockVariant.setQuantity(10); // current stock

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(item));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));

        ResOrderDTO result = orderService.cancelOrder(100);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(15, mockVariant.getQuantity()); // 10 + 5 returned
        assertEquals(800, mockCustomer.getRewardPoints()); // 1000 - 200 reverted
        verify(productVariantRepository, times(1)).save(mockVariant);
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
        verify(customerRepository, times(1)).save(mockCustomer);
    }

    @Test
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        Order order = new Order();
        order.setId(100);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> {
            orderService.cancelOrder(100);
        });
    }

    @Test
    void updatePrintStatus_Success() {
        Order order = new Order();
        order.setId(100);
        order.setPrinted(false);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.emptyList());

        ResOrderDTO result = orderService.updatePrintStatus(100, true);

        assertTrue(order.isPrinted());
        assertTrue(result.isPrinted());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void createOrder_WithPointsToUse_Success() {
        mockReqDto.setPointsToUse(50); // Mua đơn 200k, dùng 50 điểm = giảm 50k
        mockReqDto.setPaidAmount(new BigDecimal("150000"));

        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));
        when(orderRepository.countByCreatedAtAfter(any())).thenReturn(0L);

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setOrderNumber("HD-20230101-002");
        savedOrder.setCustomerId(2);
        savedOrder.setCustomerName("Nguyễn Văn A");
        savedOrder.setCreatedBy(1);
        savedOrder.setCreatedByUsername("testuser");
        savedOrder.setTotalAmount(new BigDecimal("150000")); // 200k - 50k
        savedOrder.setPaidAmount(new BigDecimal("150000"));
        savedOrder.setChangeAmount(BigDecimal.ZERO);
        savedOrder.setPointsUsed(50);
        savedOrder.setPointsEarned(150); // 150k / 1000 = 150 points
        savedOrder.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        ResOrderDTO result = orderService.createOrder(mockReqDto, "testuser");

        assertNotNull(result);
        assertEquals(new BigDecimal("150000"), result.getTotalAmount());
        assertEquals(1100, mockCustomer.getRewardPoints()); // 1000 - 50 used + 150 earned

        verify(pointHistoryRepository, times(2)).save(any(PointHistory.class));
        verify(customerRepository, times(1)).save(mockCustomer);
    }

    @Test
    void createOrder_WithVoucher_Success() {
        mockReqDto.setVoucherCode("GIAM50K"); // Mua đơn 200k, xài voucher giảm 50k
        mockReqDto.setPaidAmount(new BigDecimal("150000"));

        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));
        when(orderRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(customerVoucherRepository.findUnusedVoucherByCustomerAndCode(2, "GIAM50K"))
                .thenReturn(Optional.of(mockCustomerVoucher));

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setOrderNumber("HD-20230101-003");
        savedOrder.setCustomerId(2);
        savedOrder.setTotalAmount(new BigDecimal("150000")); // 200k - 50k
        savedOrder.setPaidAmount(new BigDecimal("150000"));
        savedOrder.setChangeAmount(BigDecimal.ZERO);
        savedOrder.setVoucherCode("GIAM50K");
        savedOrder.setDiscountFromVoucher(new BigDecimal("50000"));
        savedOrder.setPointsEarned(150);
        savedOrder.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        ResOrderDTO result = orderService.createOrder(mockReqDto, "testuser");

        assertNotNull(result);
        assertEquals(new BigDecimal("150000"), result.getTotalAmount());
        assertEquals("GIAM50K", result.getVoucherCode());
        assertEquals(new BigDecimal("50000"), result.getDiscountFromVoucher());
        assertEquals(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.USED,
                mockCustomerVoucher.getStatus());

        verify(customerVoucherRepository, times(1)).save(mockCustomerVoucher);
    }
}
