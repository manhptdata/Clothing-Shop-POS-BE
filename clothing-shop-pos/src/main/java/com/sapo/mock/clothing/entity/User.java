package com.sapo.mock.clothing.entity;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapo.mock.clothing.util.constant.RoleEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotBlank(message = "username không được để trống")
	@Column(unique = true, nullable = false, length = 50)
	private String username;

	@NotBlank(message = "password không được để trống")
	@JsonIgnore
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@NotBlank(message = "Họ tên không được để trống")
	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(length = 15, unique = true)
	private String phone;

	@NotNull(message = "Vai trò không được để trống")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoleEnum role;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@JsonIgnore
	@Column(name = "refresh_token", columnDefinition = "TEXT")
	private String refreshToken;

	@PrePersist
	public void prePersist() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return Collections.singletonList(new SimpleGrantedAuthority(this.role.name()));
	}

	@Override
	public String getPassword() {
		return this.passwordHash;
	}
}
