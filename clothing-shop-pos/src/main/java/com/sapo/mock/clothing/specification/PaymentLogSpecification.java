package com.sapo.mock.clothing.specification;

import com.sapo.mock.clothing.entity.PaymentLog;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class PaymentLogSpecification {
    public static Specification<PaymentLog> filterLogs(String orderNumber, String status, String gateway, String startDate, String endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(orderNumber)) {
                predicates.add(criteriaBuilder.like(root.get("orderNumber"), "%" + orderNumber + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(gateway)) {
                predicates.add(criteriaBuilder.equal(root.get("gateway"), gateway));
            }
            if (StringUtils.hasText(startDate)) {
                String start = startDate.length() == 10 ? startDate + " 00:00:00" : startDate;
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), start));
            }
            if (StringUtils.hasText(endDate)) {
                String end = endDate.length() == 10 ? endDate + " 23:59:59" : endDate;
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), end));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
