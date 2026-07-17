package com.sapo.mock.clothing.returnorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReqCreateReturnDTO {

    @NotNull(message = "Mã hóa đơn gốc không được trống")
    private Integer originalOrderId;

    private String reason;

    private String approvalPin;

    @NotNull(message = "Danh sách sản phẩm trả không được trống")
    @Valid
    private List<ReturnItemDTO> items;

    @Getter
    @Setter
    public static class ReturnItemDTO {
        @NotNull(message = "ID phân loại sản phẩm không được trống")
        private Integer variantId;

        @Min(value = 1, message = "Số lượng trả tối thiểu là 1")
        private int quantity;
    }
}
