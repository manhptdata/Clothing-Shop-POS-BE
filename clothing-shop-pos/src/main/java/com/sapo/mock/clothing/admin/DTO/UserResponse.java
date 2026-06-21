package com.sapo.mock.clothing.admin.DTO;

import java.time.Instant;

import com.sapo.mock.clothing.util.constant.RoleEnum;

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
	private RoleEnum role;
	private boolean active;
	private Instant createdAt;
}
