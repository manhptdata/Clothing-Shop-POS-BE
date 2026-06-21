package com.sapo.mock.clothing.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.admin.DTO.UserResponse;
import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.specification.UserSpecification;
import com.sapo.mock.clothing.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// 1. Xem danh sách nhân viên
	public Page<UserResponse> getAllEmployees(Pageable pageable, String search, Boolean active) {
		Specification<User> spec = UserSpecification.build(search, active);
		return userRepository.findAll(spec, pageable).map(this::toUserResponse);
	}

	// 2. Tạo mới nhân viên
	public UserResponse createEmployee(User user) {
		if (userRepository.existsByUsername(user.getUsername())) {
			throw new BadRequestException("username này đã tồn tại");
		}
		if (userRepository.existsByPhone(user.getPhone())) {
			throw new BadRequestException("số điện thoại này đã tồn tại");

		}
		if (user.getUsername().equalsIgnoreCase(null))
			if (user.getPasswordHash() != null) {
				user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
			}
		user.setActive(true);
		userRepository.save(user);
		return toUserResponse(user);
	}

	// 3. Khóa/Mở khóa tài khoản (Soft Delete/Active)
	@Transactional
	public void toggleEmployeeStatus(Integer userId, boolean status) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));
		user.setActive(status);
		userRepository.save(user);
	}

	// Trong UserService.java
	public UserResponse getEmployeeDetail(Integer id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại"));

		return toUserResponse(user);
	}

	private UserResponse toUserResponse(User user) {
		return UserResponse.builder().id(user.getId()).username(user.getUsername()).fullName(user.getFullName())
				.phone(user.getPhone()).role(user.getRole()).active(user.isActive()).createdAt(user.getCreatedAt())
				.build();
	}

}
