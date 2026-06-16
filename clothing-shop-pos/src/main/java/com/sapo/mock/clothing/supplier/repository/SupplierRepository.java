package com.sapo.mock.clothing.supplier.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer>, JpaSpecificationExecutor<Supplier> {
	Optional<Supplier> findByIdAndActiveTrue(Integer id);
}