package com.sapo.mock.clothing.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqCancelOrderDTO {
    
    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
    
    private String approvalPin;
}
