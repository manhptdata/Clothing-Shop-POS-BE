package com.sapo.mock.clothing.product.service;

import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.entity.ProductAttribute;
import com.sapo.mock.clothing.product.DTO.ProductAttributeResponse;

@Service
public class ProductAttributeService {

	public ProductAttributeResponse toProductAttributesResponse(ProductAttribute attribute) {
		if (attribute == null) {
			return null;
		}

		ProductAttributeResponse response = new ProductAttributeResponse();
		response.setId(attribute.getId());

		if (attribute.getProduct() != null) {
			response.setProductID(attribute.getProduct().getId());
		}

		response.setAttrKey(attribute.getAttrKey());
		response.setAttrValue(attribute.getAttrValue());

		return response;
	}
}