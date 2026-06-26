package com.sapo.mock.clothing.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.sapo.mock.clothing.entity.StockReceipt;
import com.sapo.mock.clothing.util.constant.ReceiptStatus;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class StockReceiptSpecification {

	public static Specification<StockReceipt> filterReceipts(String search, ReceiptStatus status) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Filter by status if provided
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			// Filter by search keyword (e.g. matching code or note)
			if (StringUtils.hasText(search)) {
				String likePattern = "%" + search.trim().toLowerCase() + "%";
				Predicate codePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), likePattern);
				Predicate notePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), likePattern);
				predicates.add(criteriaBuilder.or(codePredicate, notePredicate));
			}

			// Add sorting if needed, but Pageable usually handles this
			// Default sort by createdAt desc is typically handled by controller's Pageable

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
