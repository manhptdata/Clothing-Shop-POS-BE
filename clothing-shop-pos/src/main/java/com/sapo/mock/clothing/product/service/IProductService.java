package com.sapo.mock.clothing.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sapo.mock.clothing.product.DTO.ProductRequest;
import com.sapo.mock.clothing.product.DTO.ProductResponse;

public interface IProductService {

	Page<ProductResponse> getAllProducts(Pageable pageable, String search, String productName, String sku,
			Integer categoryID, Boolean isDeleted, String stockStatus);

	ProductResponse creatProduct(ProductRequest request, String username);

	ProductResponse updateProduct(Integer id, ProductRequest request);

	ProductResponse deleteProduct(Integer id);

	void hardDeleteProduct(Integer id);

	ProductResponse getProductByID(Integer id);

}
