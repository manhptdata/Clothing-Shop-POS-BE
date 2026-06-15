package com.sapo.mock.clothing.supplier.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierRequest {
	@NotBlank(message = "Tên nhà cung cấp không được để trống")
	private String name;

	private String phone;

	@Email(message = "Email không đúng định dạng")
	private String email;

	private String address;

	private String note;
}