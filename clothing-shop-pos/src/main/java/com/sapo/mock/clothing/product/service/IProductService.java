package com.sapo.mock.clothing.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sapo.mock.clothing.product.DTO.ProductRequest;
import com.sapo.mock.clothing.product.DTO.ProductResponse;

public interface IProductService {

	Page<ProductResponse> getAllProducts(Pageable pageable, String search, String productName, String sku,
			String category);

	ProductResponse creatProduct(ProductRequest request, String username);

	ProductResponse updateProduct(Integer id, ProductRequest request);

}
