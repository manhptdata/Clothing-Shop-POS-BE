package com.sapo.mock.clothing.admin.service;

import org.springframework.cache.annotation.CacheEvict;
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
import com.sapo.mock.clothing.user.repository.RoleRepository;
import com.sapo.mock.clothing.entity.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RoleRepository roleRepository;

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
		String phone = request.getPhone();
		if (phone != null && phone.trim().isEmpty()) {
			phone = null;
		}
		if (phone != null && userRepository.existsByPhone(phone)) {
			throw new BadRequestException("Số điện thoại này đã tồn tại");
		}

		Role role = roleRepository.findById(request.getRoleId())
				.orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ"));

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setFullName(request.getFullName());
		user.setPhone(phone);
		user.setRole(role);
		user.setActive(true);

		userRepository.save(user);
		return toUserResponse(user);
	}

	// Cập nhật thông tin nhân viên
	@CacheEvict(value = "users", allEntries = true)
	@Transactional
	public UserResponse updateEmployee(Integer id, UserUpdateRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));

		String phone = request.getPhone();
		if (phone != null && phone.trim().isEmpty()) {
			phone = null;
		}

		if (phone != null && userRepository.existsByPhoneAndIdNot(phone, id)) {
			throw new BadRequestException("Số điện thoại này đã được sử dụng bởi nhân viên khác");
		}

		Role role = roleRepository.findById(request.getRoleId())
				.orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ"));

		user.setFullName(request.getFullName());
		user.setPhone(phone);
		user.setRole(role);

		userRepository.save(user);
		return toUserResponse(user);
	}

	// 3. Khóa/Mở khóa tài khoản (Soft Delete/Active)
	@CacheEvict(value = "users", allEntries = true)
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
		String roleName = user.getRole() != null ? user.getRole().getName() : null;
		Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
		return UserResponse.builder().id(user.getId()).username(user.getUsername()).fullName(user.getFullName())
				.phone(user.getPhone()).role(roleName).active(user.isActive()).createdAt(user.getCreatedAt())
				.build();
	}

}
