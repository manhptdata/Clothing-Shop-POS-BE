package com.sapo.mock.clothing.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.util.constant.OrderStatus;

import jakarta.persistence.criteria.Predicate;

public class OrderSpecification {

	public static Specification<Order> build(OrderStatus status) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
