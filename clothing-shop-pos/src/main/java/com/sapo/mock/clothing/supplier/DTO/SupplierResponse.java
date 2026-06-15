package com.sapo.mock.clothing.supplier.DTO;

import java.time.Instant;

import lombok.Data;

@Data
public class SupplierResponse {
	private Integer id;
	private String name;
	private String phone;
	private String email;
	private String address;
	private String note;
	private boolean active;
	private Instant createdAt;
	private Instant updatedAt;
}