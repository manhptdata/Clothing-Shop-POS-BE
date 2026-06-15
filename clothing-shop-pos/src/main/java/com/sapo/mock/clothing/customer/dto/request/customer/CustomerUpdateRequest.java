package com.sapo.mock.clothing.customer.dto.request.customer;


import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.GenderEnum;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerUpdateRequest {

    @NotBlank(message = "Họ tên khách hàng không được để trống")
    @Size(max = 200, message = "Họ tên không được vượt quá 200 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự")
    private String phone;

    @PastOrPresent(message = "Ngày sinh không được là một ngày trong tương lai")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private GenderEnum gender;

    private String address;
    private String note;

    @NotNull(message = "Trạng thái không được để trống")
    private CustomerStatusEnum status;
}