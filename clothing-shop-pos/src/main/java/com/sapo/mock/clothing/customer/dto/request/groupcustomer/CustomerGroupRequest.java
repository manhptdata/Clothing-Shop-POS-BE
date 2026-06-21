package com.sapo.mock.clothing.customer.dto.request.groupcustomer;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class CustomerGroupRequest {

    @NotBlank(message = "Tên nhóm khách hàng không được để trống")
    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    private String name;

    private String description;

    private CustomerStatusEnum status = CustomerStatusEnum.ACTIVE;
    private String note;
    private BigDecimal minSpending;
    private BigDecimal maxSpending;

    @NotNull(message = "Mã phân hạng (code) không được để trống")
    private RankCodeEnum code;
}
