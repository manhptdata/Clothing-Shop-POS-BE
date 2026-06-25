package com.sapo.mock.clothing.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapo.mock.clothing.admin.DTO.UserCreateRequest;
import com.sapo.mock.clothing.admin.DTO.UserResponse;
import com.sapo.mock.clothing.admin.DTO.UserUpdateRequest;
import com.sapo.mock.clothing.admin.service.AdminService;
import com.sapo.mock.clothing.common.dto.response.RestResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminUserController {

	@Autowired
	private AdminService adminService;

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<RestResponse<UserResponse>> getEmployeeDetail(@PathVariable Integer id) {
		UserResponse data = adminService.getEmployeeDetail(id);
		return ResponseEntity.ok(new RestResponse<>(200, null, "Lấy chi tiết nhân viên thành công", data));
	}

	@GetMapping
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<RestResponse<Page<UserResponse>>> getAllEmployees(Pageable pageable,
			@RequestParam(required = false) String search, @RequestParam(required = false) Boolean active) {

		Page<UserResponse> users = adminService.getAllEmployees(pageable, search, active);
		RestResponse<Page<UserResponse>> response = new RestResponse<>(200, null, "Lấy danh sách sản phẩm thành công",
				users);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<RestResponse<UserResponse>> createEmployee(@Valid @RequestBody UserCreateRequest request) {
		UserResponse createdUser = adminService.createEmployee(request);
		return ResponseEntity.ok(new RestResponse<>(201, null, "Tạo nhân viên thành công", createdUser));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<RestResponse<UserResponse>> updateEmployee(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request) {
		UserResponse updatedUser = adminService.updateEmployee(id, request);
		return ResponseEntity.ok(new RestResponse<>(200, null, "Cập nhật thông tin nhân viên thành công", updatedUser));
	}

	// Khóa tài khoản (Gửi status = false)
	@PutMapping("/{id}/status")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<RestResponse<String>> updateStatus(@PathVariable Integer id, @RequestParam boolean isActive) {
		adminService.toggleEmployeeStatus(id, isActive);
		return ResponseEntity.ok(new RestResponse<>(200, null, "Cập nhật trạng thái thành công", null));
	}
}