package com.sapo.mock.clothing.admin.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String phone;

    private String email;

    @NotNull(message = "Vai trò không được để trống")
    private Integer roleId;
}
