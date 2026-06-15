package com.sapo.mock.clothing.supplier.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.supplier.DTO.SupplierRequest;
import com.sapo.mock.clothing.supplier.DTO.SupplierResponse;
import com.sapo.mock.clothing.supplier.service.ISupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

	private final ISupplierService supplierService;

	@GetMapping
	public ResponseEntity<RestResponse<Page<SupplierResponse>>> getAllSuppliers(Pageable pageable,
			@RequestParam(required = false) String search, @RequestParam(required = false) Boolean isActive) {
		Page<SupplierResponse> suppliers = supplierService.getAllSuppliers(pageable, search, isActive);
		RestResponse<Page<SupplierResponse>> response = new RestResponse<>(200, null,
				"Lấy danh sách nhà cung cấp thành công", suppliers);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<RestResponse<SupplierResponse>> getSupplierById(@PathVariable Integer id) {
		SupplierResponse supplier = supplierService.getSupplierById(id);
		RestResponse<SupplierResponse> response = new RestResponse<>(200, null, "Lấy thông tin nhà cung cấp thành công",
				supplier);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<RestResponse<SupplierResponse>> createSupplier(@Valid @RequestBody SupplierRequest request) {
		SupplierResponse supplier = supplierService.createSupplier(request);
		RestResponse<SupplierResponse> response = new RestResponse<>(201, null, "Tạo nhà cung cấp thành công",
				supplier);
		return ResponseEntity.status(201).body(response);
	}

	@PutMapping("/{id}")
	public ResponseEntity<RestResponse<SupplierResponse>> updateSupplier(@PathVariable Integer id,
			@Valid @RequestBody SupplierRequest request) {
		SupplierResponse supplier = supplierService.updateSupplier(id, request);
		RestResponse<SupplierResponse> response = new RestResponse<>(200, null, "Cập nhật nhà cung cấp thành công",
				supplier);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RestResponse<SupplierResponse>> deleteSupplier(@PathVariable Integer id) {
		SupplierResponse supplier = supplierService.deleteSupplier(id);
		RestResponse<SupplierResponse> response = new RestResponse<>(200, null,
				"Ngừng hoạt động nhà cung cấp thành công", supplier);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<RestResponse<Void>> hardDeleteSupplier(@PathVariable Integer id) {
		supplierService.hardDeleteSupplier(id);
		RestResponse<Void> response = new RestResponse<>(200, null, "Xóa vĩnh viễn nhà cung cấp thành công", null);
		return ResponseEntity.ok(response);
	}
}