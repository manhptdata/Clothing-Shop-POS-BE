package com.sapo.mock.clothing.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sapo.mock.clothing.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
	boolean existsBySku(String sku);
}
