package com.sapo.mock.clothing.supplier.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sapo.mock.clothing.supplier.DTO.SupplierRequest;
import com.sapo.mock.clothing.supplier.DTO.SupplierResponse;

public interface ISupplierService {
	Page<SupplierResponse> getAllSuppliers(Pageable pageable, String search, Boolean isActive);

	SupplierResponse getSupplierById(Integer id);

	SupplierResponse createSupplier(SupplierRequest request);

	SupplierResponse updateSupplier(Integer id, SupplierRequest request);

	SupplierResponse deleteSupplier(Integer id);

	SupplierResponse reactivateSupplier(Integer id);

	void hardDeleteSupplier(Integer id);
}