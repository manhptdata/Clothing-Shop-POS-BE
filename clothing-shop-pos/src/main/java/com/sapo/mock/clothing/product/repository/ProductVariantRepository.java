package com.sapo.mock.clothing.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.ProductVariant;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
	boolean existsBySku(String sku);
	boolean existsBySkuAndIsActiveTrue(String sku);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
	Optional<ProductVariant> findByIdWithPessimisticLock(@Param("id") Integer id);
}
