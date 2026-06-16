package com.sapo.mock.clothing.product.DTO;

import java.util.List;

import lombok.Data;

@Data
public class ProductOptionResponse {
	private Integer id;
	private String name;
	private Integer position;
	private List<String> values;
}