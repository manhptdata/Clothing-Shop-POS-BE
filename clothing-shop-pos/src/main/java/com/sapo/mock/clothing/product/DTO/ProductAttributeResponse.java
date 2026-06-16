package com.sapo.mock.clothing.product.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductAttributeResponse {
	private Integer id;
	private Integer productID;
	private String attrKey;
	private String attrValue;
}