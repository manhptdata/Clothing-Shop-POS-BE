package com.sapo.mock.clothing.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
	boolean existsBySku(String sku);
	boolean existsBySkuAndIsActiveTrue(String sku);
}
