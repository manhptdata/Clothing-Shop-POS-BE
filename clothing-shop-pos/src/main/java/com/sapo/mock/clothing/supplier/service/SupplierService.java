package com.sapo.mock.clothing.supplier.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapo.mock.clothing.entity.Supplier;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.specification.SupplierSpecification;
import com.sapo.mock.clothing.supplier.DTO.SupplierRequest;
import com.sapo.mock.clothing.supplier.DTO.SupplierResponse;
import com.sapo.mock.clothing.supplier.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService implements ISupplierService {

	private final SupplierRepository supplierRepository;

	// Helper method map Entity -> Response
	private SupplierResponse toResponse(Supplier supplier) {
		if (supplier == null)
			return null;
		SupplierResponse response = new SupplierResponse();
		response.setId(supplier.getId());
		response.setName(supplier.getName());
		response.setPhone(supplier.getPhone());
		response.setEmail(supplier.getEmail());
		response.setAddress(supplier.getAddress());
		response.setNote(supplier.getNote());
		response.setActive(supplier.isActive());
		response.setCreatedAt(supplier.getCreatedAt());
		response.setUpdatedAt(supplier.getUpdatedAt());
		return response;
	}

	@Override
	public Page<SupplierResponse> getAllSuppliers(Pageable pageable, String search, Boolean isActive) {

		Specification<Supplier> spec = SupplierSpecification.build(search, isActive);

		return supplierRepository.findAll(spec, pageable).map(this::toResponse);
	}

	@Override
	public SupplierResponse getSupplierById(Integer id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Nhà cung cấp"));
		return toResponse(supplier);
	}

	@Override
	@Transactional
	public SupplierResponse createSupplier(SupplierRequest request) {
		if (request.getPhone() != null && !request.getPhone().isBlank() && supplierRepository.existsByPhone(request.getPhone())) {
			throw new BadRequestException("Số điện thoại đã tồn tại");
		}
		if (request.getEmail() != null && !request.getEmail().isBlank() && supplierRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email đã tồn tại");
		}

		Supplier supplier = new Supplier();
		supplier.setName(request.getName());
		supplier.setPhone(request.getPhone());
		supplier.setEmail(request.getEmail());
		supplier.setAddress(request.getAddress());
		supplier.setNote(request.getNote());
		supplier.setActive(true); // Mặc định hoạt động khi tạo mới

		Supplier savedSupplier = supplierRepository.save(supplier);
		return toResponse(savedSupplier);
	}

	@Override
	@Transactional
	public SupplierResponse updateSupplier(Integer id, SupplierRequest request) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Nhà cung cấp"));

		if (request.getPhone() != null && !request.getPhone().isBlank() && supplierRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
			throw new BadRequestException("Số điện thoại đã tồn tại");
		}
		if (request.getEmail() != null && !request.getEmail().isBlank() && supplierRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
			throw new BadRequestException("Email đã tồn tại");
		}

		supplier.setName(request.getName());
		supplier.setPhone(request.getPhone());
		supplier.setEmail(request.getEmail());
		supplier.setAddress(request.getAddress());
		supplier.setNote(request.getNote());

		Supplier updatedSupplier = supplierRepository.save(supplier);
		return toResponse(updatedSupplier);
	}

	@Override
	@Transactional
	public SupplierResponse deleteSupplier(Integer id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Nhà cung cấp"));

		if (!supplier.isActive()) {
			throw new BadRequestException("Nhà cung cấp này đã bị ngừng hoạt động từ trước");
		}

		// Xóa mềm: Chuyển cờ active thành false
		supplier.setActive(false);
		supplierRepository.save(supplier);
		return toResponse(supplier);
	}

	@Override
	@Transactional
	public void hardDeleteSupplier(Integer id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Nhà cung cấp"));
		if (supplier.isActive()) {
			throw new BadRequestException("Nhà cung cấp này vẫn đang hợp tác không thể xóa, hãy ngừng hợp tác trước");
		}
		try {
			supplierRepository.delete(supplier);
		} catch (DataIntegrityViolationException e) {
			// Bắt lỗi khi Nhà cung cấp đã dính khóa ngoại (Đã có giao dịch nhập hàng)
			throw new BadRequestException(
					"Không thể xóa cứng! Nhà cung cấp này đã có lịch sử giao dịch. Vui lòng dùng chức năng Xóa mềm (Ngừng hoạt động).");
		}
	}
}