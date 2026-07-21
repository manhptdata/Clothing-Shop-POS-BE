package com.sapo.mock.clothing.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
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

    @Min(value = 0, message = "Số tiền thanh toán không được âm")
    private BigDecimal paidAmount;

    private com.sapo.mock.clothing.util.constant.OrderStatus status;

    private String note;

    private String paymentMethod;

    @Min(value = 0, message = "Số điểm sử dụng không được âm")
    private Integer pointsToUse = 0; // Số điểm muốn sử dụng trong đơn hàng này

    private String voucherCode; // Mã voucher muốn áp dụng

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    private List<OrderItemDTO> items;

    @Getter
    @Setter
    public static class OrderItemDTO {
        @NotNull(message = "ID phân loại sản phẩm không được trống")
        private Integer variantId;

        @NotNull(message = "Số lượng sản phẩm không được trống")
        @Min(value = 1, message = "Số lượng sản phẩm phải lớn hơn 0")
        private Integer quantity;
    }
}
