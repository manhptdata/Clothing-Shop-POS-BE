package com.sapo.mock.clothing.customer.dto.request.groupcustomer;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.GenderEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 200, message = "Tên không được vượt quá 200 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 15, message = "Số điện thoại không hợp lệ")
    private String phone;

    private LocalDate dateOfBirth;

    private GenderEnum gender;

    private String address;

    private String note;

    private CustomerStatusEnum status;

    private Integer customerGroupId;
}