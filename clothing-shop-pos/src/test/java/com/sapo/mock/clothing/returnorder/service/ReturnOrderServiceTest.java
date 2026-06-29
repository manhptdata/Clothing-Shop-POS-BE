package com.sapo.mock.clothing.returnorder.service;

import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import com.sapo.mock.clothing.entity.*;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.returnorder.dto.ReqCreateReturnDTO;
import com.sapo.mock.clothing.returnorder.dto.ResReturnOrderDTO;
import com.sapo.mock.clothing.returnorder.repository.ReturnOrderLineItemRepository;
import com.sapo.mock.clothing.returnorder.repository.ReturnOrderRepository;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReturnOrderServiceTest {

    @Mock
    private ReturnOrderRepository returnOrderRepository;
    @Mock
    private ReturnOrderLineItemRepository returnOrderLineItemRepository;
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
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReturnOrderService returnOrderService;

    private User mockUser;
    private Customer mockCustomer;
    private Order mockOrder;
    private OrderLineItem mockLineItem;
    private ProductVariant mockVariant;
    private ReqCreateReturnDTO mockReqDto;
    private CustomerVoucher mockCustomerVoucher;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("sale01");

        mockCustomer = new Customer();
        mockCustomer.setId(2);
        mockCustomer.setFullName("Nguyễn Văn A");
        mockCustomer.setRewardPoints(500);

        mockOrder = new Order();
        mockOrder.setId(100);
        mockOrder.setOrderNumber("HD-20260629-001");
        mockOrder.setCustomerId(mockCustomer.getId());
        mockOrder.setCustomerName(mockCustomer.getFullName());
        mockOrder.setTotalAmount(new BigDecimal("180000")); // 200k original - 20k voucher discount
        mockOrder.setPaidAmount(new BigDecimal("180000"));
        mockOrder.setDiscountFromVoucher(new BigDecimal("20000"));
        mockOrder.setDiscountFromPoints(BigDecimal.ZERO);
        mockOrder.setStatus(OrderStatus.COMPLETED);
        mockOrder.setCreatedAt(Instant.now());

        mockLineItem = new OrderLineItem();
        mockLineItem.setId(1);
        mockLineItem.setOrder(mockOrder);
        mockLineItem.setVariantId(10);
        mockLineItem.setProductName("Áo khoác");
        mockLineItem.setProductSku("AK-01");
        mockLineItem.setQuantity(2);
        mockLineItem.setUnitPrice(new BigDecimal("100000")); // original subtotal = 200k

        mockVariant = new ProductVariant();
        mockVariant.setId(10);
        mockVariant.setSku("AK-01");
        mockVariant.setQuantity(5); // current stock
        mockVariant.setSalePrice(new BigDecimal("100000"));

        mockReqDto = new ReqCreateReturnDTO();
        mockReqDto.setOriginalOrderId(100);
        mockReqDto.setReason("Hàng lỗi");
        ReqCreateReturnDTO.ReturnItemDTO returnItem = new ReqCreateReturnDTO.ReturnItemDTO();
        returnItem.setVariantId(10);
        returnItem.setQuantity(2);
        mockReqDto.setItems(Collections.singletonList(returnItem));

        Voucher voucher = new Voucher();
        voucher.setId(1);
        voucher.setCode("GIAM20K");
        voucher.setDiscountAmount(new BigDecimal("20000"));

        mockCustomerVoucher = new CustomerVoucher();
        mockCustomerVoucher.setId(1);
        mockCustomerVoucher.setCustomer(mockCustomer);
        mockCustomerVoucher.setVoucher(voucher);
        mockCustomerVoucher.setStatus(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.USED);
    }

    @Test
    void createReturn_Success_FullReturn() {
        when(userRepository.findByUsername("sale01")).thenReturn(mockUser);
        when(orderRepository.findById(100)).thenReturn(Optional.of(mockOrder));
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(mockLineItem));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));
        when(returnOrderRepository.findByOrderId(100)).thenReturn(Collections.emptyList());
        
        // Mock save
        ReturnOrder savedReturnOrder = new ReturnOrder();
        savedReturnOrder.setId(1);
        savedReturnOrder.setOrder(mockOrder);
        savedReturnOrder.setCustomerId(2);
        savedReturnOrder.setCustomerName("Nguyễn Văn A");
        savedReturnOrder.setTotalRefundAmount(new BigDecimal("180000")); // rounded and capped at 180k
        savedReturnOrder.setReturnNumber("RTN-20260629-001");
        savedReturnOrder.setReason("Hàng lỗi");
        savedReturnOrder.setCreatedAt(Instant.now());

        when(returnOrderRepository.save(any(ReturnOrder.class))).thenReturn(savedReturnOrder);
        when(customerVoucherRepository.findByOrderId(100)).thenReturn(Optional.of(mockCustomerVoucher));

        ResReturnOrderDTO result = returnOrderService.createReturn(mockReqDto, "sale01");

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("RTN-20260629-001", result.getReturnNumber());
        assertEquals(new BigDecimal("180000"), result.getTotalRefundAmount());
        assertEquals(OrderStatus.RETURNED, mockOrder.getStatus()); // full return changes status to RETURNED
        assertEquals(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.UNUSED, mockCustomerVoucher.getStatus()); // voucher reverted
        assertEquals(7, mockVariant.getQuantity()); // 5 + 2 restocked
        assertEquals(320, mockCustomer.getRewardPoints()); // 500 - (180000 / 1000) = 500 - 180 = 320 points

        verify(productVariantRepository, times(1)).save(mockVariant);
        verify(customerRepository, times(1)).save(mockCustomer);
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
        verify(customerVoucherRepository, times(1)).save(mockCustomerVoucher);
        verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void createReturn_Success_PartialReturn() {
        // Partial return: only return 1 item instead of 2
        mockReqDto.getItems().get(0).setQuantity(1);

        when(userRepository.findByUsername("sale01")).thenReturn(mockUser);
        when(orderRepository.findById(100)).thenReturn(Optional.of(mockOrder));
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(mockLineItem));
        when(productVariantRepository.findById(10)).thenReturn(Optional.of(mockVariant));
        when(returnOrderRepository.findByOrderId(100)).thenReturn(Collections.emptyList());

        ReturnOrder savedReturnOrder = new ReturnOrder();
        savedReturnOrder.setId(1);
        savedReturnOrder.setOrder(mockOrder);
        savedReturnOrder.setCustomerId(2);
        savedReturnOrder.setTotalRefundAmount(new BigDecimal("90000")); // 100k - (20k discount * 100k / 200k) = 90k
        savedReturnOrder.setCreatedAt(Instant.now());

        when(returnOrderRepository.save(any(ReturnOrder.class))).thenReturn(savedReturnOrder);

        ResReturnOrderDTO result = returnOrderService.createReturn(mockReqDto, "sale01");

        assertNotNull(result);
        assertEquals(new BigDecimal("90000"), result.getTotalRefundAmount());
        assertEquals(OrderStatus.PARTIALLY_RETURNED, mockOrder.getStatus()); // partial return
        assertEquals(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.USED, mockCustomerVoucher.getStatus()); // voucher NOT reverted
        assertEquals(6, mockVariant.getQuantity()); // 5 + 1 restocked
        assertEquals(410, mockCustomer.getRewardPoints()); // 500 - 90 = 410 points
    }

    @Test
    void createReturn_ExpiredTimeLimit_ThrowsException() {
        // Order created 8 days ago
        mockOrder.setCreatedAt(Instant.now().minus(8, ChronoUnit.DAYS));

        when(userRepository.findByUsername("sale01")).thenReturn(mockUser);
        when(orderRepository.findById(100)).thenReturn(Optional.of(mockOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            returnOrderService.createReturn(mockReqDto, "sale01");
        });
        assertTrue(ex.getMessage().contains("quá 7 ngày"));
    }

    @Test
    void createReturn_ReturnLimitExceeded_ThrowsException() {
        // Return 3 items while only purchased 2
        mockReqDto.getItems().get(0).setQuantity(3);

        when(userRepository.findByUsername("sale01")).thenReturn(mockUser);
        when(orderRepository.findById(100)).thenReturn(Optional.of(mockOrder));
        when(customerRepository.findById(2)).thenReturn(Optional.of(mockCustomer));
        when(orderLineItemRepository.findByOrderId(100)).thenReturn(Collections.singletonList(mockLineItem));
        when(returnOrderRepository.findByOrderId(100)).thenReturn(Collections.emptyList());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            returnOrderService.createReturn(mockReqDto, "sale01");
        });
        assertTrue(ex.getMessage().contains("vượt quá số lượng còn lại"));
    }
}
