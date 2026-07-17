package com.sapo.mock.clothing.entity;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapo.mock.clothing.entity.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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

	@JsonProperty("password")
	@NotBlank(message = "password không được để trống")
//	@JsonIgnore
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@NotBlank(message = "Họ tên không được để trống")
	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(length = 15, unique = true)
	private String phone;

	@Column(length = 100, unique = true)
	private String email;

	@ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
	@JoinColumn(name = "role_id")
	private Role role;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@JsonIgnore
	@Column(name = "refresh_token", columnDefinition = "TEXT")
	private String refreshToken;

	@JsonIgnore
	@Column(name = "security_pin")
	private String securityPin;

	@PrePersist
	public void prePersist() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (this.role != null && this.role.getPermissions() != null) {
			return this.role.getPermissions().stream()
					.map(permission -> new SimpleGrantedAuthority(permission.name()))
					.toList();
		}
		return Collections.emptyList();
	}

	@JsonProperty("password")
	public void setPassword(String password) {
		this.passwordHash = password;
	}

	@JsonIgnore
	public String getPasswordHash() {
		return this.passwordHash;
	}

	@Override
	@JsonIgnore
	public String getPassword() {
		return this.passwordHash;
	}

	@Override
	public boolean isEnabled() {
		return this.active;
	}
}
