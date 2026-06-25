package com.sapo.mock.clothing.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.admin.DTO.UserCreateRequest;
import com.sapo.mock.clothing.admin.DTO.UserResponse;
import com.sapo.mock.clothing.admin.DTO.UserUpdateRequest;
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
	@Transactional
	public UserResponse createEmployee(UserCreateRequest request) {
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new BadRequestException("Username này đã tồn tại");
		}
		if (request.getPhone() != null && !request.getPhone().isEmpty() && userRepository.existsByPhone(request.getPhone())) {
			throw new BadRequestException("Số điện thoại này đã tồn tại");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setFullName(request.getFullName());
		user.setPhone(request.getPhone());
		user.setRole(request.getRole());
		user.setActive(true);

		userRepository.save(user);
		return toUserResponse(user);
	}

	// Cập nhật thông tin nhân viên
	@Transactional
	public UserResponse updateEmployee(Integer id, UserUpdateRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));

		if (request.getPhone() != null && !request.getPhone().isEmpty() && userRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
			throw new BadRequestException("Số điện thoại này đã được sử dụng bởi nhân viên khác");
		}

		user.setFullName(request.getFullName());
		user.setPhone(request.getPhone());
		user.setRole(request.getRole());

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
