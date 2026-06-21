package com.sapo.mock.clothing.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ReqCreateOrderDTO {

    @NotNull(message = "Customer ID không được để trống")
    private Integer customerId;


    @NotNull(message = "Số tiền khách trả không được để trống")
    private BigDecimal paidAmount;

    private String note;

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
