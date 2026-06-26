package com.sapo.mock.clothing.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.Category;
import com.sapo.mock.clothing.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
	boolean existsByCategory_Id(Integer categoryId);

	boolean existsByName(String name);

	@Modifying
	@Query("UPDATE Product p SET p.category = :newCategory WHERE p.category.id = :oldCategoryId")
	void updateCategoryForProducts(@Param("oldCategoryId") Integer oldCategoryId, @Param("newCategory") Category newCategory);
}
