package com.sapo.mock.clothing.product.service;

import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.product.DTO.ProductAttributeResponse;

@Service
public class ProductAttributeService implements IProductAttributeService {

	public ProductAttributeResponse toProductAttributesResponse(
			com.sapo.mock.clothing.entity.ProductAttribute productAttribute) {
		try {
			ProductAttributeResponse productAttributesResponse = new ProductAttributeResponse();
			productAttributesResponse.setId(productAttribute.getId());
			productAttributesResponse.setAttrKey(productAttribute.getAttrKey());
			productAttributesResponse.setAttrValue(productAttribute.getAttrValue());
			productAttributesResponse.setProductID(productAttribute.getProduct().getId());

			return productAttributesResponse;
		} catch (Exception e) {
			return null;

		}

	}

}
