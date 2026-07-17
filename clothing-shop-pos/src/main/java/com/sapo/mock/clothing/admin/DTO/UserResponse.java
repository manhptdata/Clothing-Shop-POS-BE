package com.sapo.mock.clothing.admin.DTO;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
	private Integer id;
	private String username;
	private String fullName;
	private String phone;
	private String email;
	private String role;
	private boolean active;
	private Instant createdAt;
}
