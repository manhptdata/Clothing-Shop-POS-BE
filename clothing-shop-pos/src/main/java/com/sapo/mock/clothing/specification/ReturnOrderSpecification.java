package com.sapo.mock.clothing.specification;

import com.sapo.mock.clothing.entity.ReturnOrder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReturnOrderSpecification {

    public static Specification<ReturnOrder> build(String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate returnNoLike = cb.like(cb.lower(root.get("returnNumber")), likePattern);
                Predicate customerLike = cb.like(cb.lower(root.get("customerName")), likePattern);
                Predicate orderNoLike = cb.like(cb.lower(root.get("order").get("orderNumber")), likePattern);
                predicates.add(cb.or(returnNoLike, customerLike, orderNoLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
