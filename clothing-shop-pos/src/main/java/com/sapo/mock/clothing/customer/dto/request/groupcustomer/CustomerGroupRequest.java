package com.sapo.mock.clothing.customer.dto.request.groupcustomer;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomerGroupRequest {

    @NotBlank(message = "Tên nhóm không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Mã hạng (Rank) không được để trống")
    private RankCodeEnum code;

    @NotNull(message = "Chi tiêu tối thiểu không được để trống")
    private BigDecimal minSpending;


    private Integer birthdayVoucherId; // ID của Voucher sinh nhật (chọn từ dropdown trên UI)

    private CustomerStatusEnum status;

    private String note;
}
