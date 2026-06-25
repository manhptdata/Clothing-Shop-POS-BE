package com.sapo.mock.clothing.admin.DTO;

import com.sapo.mock.clothing.util.constant.RoleEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    private RoleEnum role;
}
