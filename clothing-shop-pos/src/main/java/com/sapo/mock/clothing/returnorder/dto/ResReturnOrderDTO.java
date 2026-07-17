package com.sapo.mock.clothing.returnorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResReturnOrderDTO {
    private Integer id;
    private String returnNumber;
    private Integer originalOrderId;
    private String originalOrderNumber;
    private Integer customerId;
    private String customerName;
    private Integer createdById;
    private String createdByUsername;
    private String approvedByUsername;
    private BigDecimal totalRefundAmount;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ResReturnOrderItemDTO> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResReturnOrderItemDTO {
        private Integer id;
        private Integer variantId;
        private String productName;
        private String productSku;
        private int quantity;
        private BigDecimal refundPrice;
        private BigDecimal subtotal;
    }
}
