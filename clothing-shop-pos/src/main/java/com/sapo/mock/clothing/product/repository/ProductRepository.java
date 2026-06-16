package com.sapo.mock.clothing.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
	boolean existsByCategory_Id(Integer categoryId);

	boolean existsByName(String name);
}
