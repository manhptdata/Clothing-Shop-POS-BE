package com.sapo.mock.clothing.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sapo.mock.clothing.entity.Product;

import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {
	public static Specification<Product> build(String search, String productName, String sku, String category) {
//		nếu viến hẳn ra nó sẽ thế này
//		return (Root<Products> root,
//		        CriteriaQuery<?> query,
//		        CriteriaBuilder cb) ->

//		đây là kiểu biết ngắn gọn
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (search != null && !search.trim().isEmpty()) {
				String pattern = "%" + search.trim().toLowerCase() + "%";

				Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
				Predicate skuLike = cb.like(cb.lower(root.get("sku")), pattern);

				predicates.add(cb.or(nameLike, skuLike));

			}
//			filter theo tên product
			if (productName != null && !productName.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + productName.toLowerCase().trim() + "%"));

			}

			if (sku != null && !sku.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("sku")), "%" + sku.toLowerCase().trim() + "%"));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

	}
}
