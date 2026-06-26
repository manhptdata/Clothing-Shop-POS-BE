package com.sapo.mock.clothing.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqCreateOrderDTO {

    @NotNull(message = "Customer ID không được để trống")
    private Integer customerId;


    private BigDecimal paidAmount;

    private com.sapo.mock.clothing.util.constant.OrderStatus status;

    private String note;

    private String paymentMethod;

    private Integer pointsToUse = 0; // Số điểm muốn sử dụng trong đơn hàng này

    private String voucherCode; // Mã voucher muốn áp dụng


    private List<OrderItemDTO> items;

    @Getter
    @Setter
    public static class OrderItemDTO {
        private Integer variantId;
        private Integer quantity;
    }
}
