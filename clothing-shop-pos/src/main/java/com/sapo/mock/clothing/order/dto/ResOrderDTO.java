package com.sapo.mock.clothing.order.dto;

import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.util.constant.PaymentMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
public class ResOrderDTO {

    private Integer id;
    private String orderNumber;

    private Integer customerId;
    private String customerName;


    private Integer createdById;
    private String createdByUsername;

    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;

    private Integer pointsUsed;
    private Integer pointsEarned;
    private BigDecimal discountFromPoints;

    private String voucherCode;
    private BigDecimal discountFromVoucher;
    private BigDecimal voucherMinOrderValue;

    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private boolean isPrinted;
    private String note;

    private Instant createdAt;
    private Instant updatedAt;

    private List<ResOrderItemDTO> items;

    @Getter
    @Setter
    @Builder
    public static class ResOrderItemDTO {
        private Integer id;
        private Integer variantId;
        private String productName;
        private String productSku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
