package com.sapo.mock.clothing.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sapo.mock.clothing.entity.Supplier;

import jakarta.persistence.criteria.Predicate;

public class SupplierSpecification {

	public static Specification<Supplier> build(String search, Boolean isActive) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// 1. Tìm kiếm tương đối theo Tên hoặc Số điện thoại
			if (search != null && !search.trim().isEmpty()) {
				String searchKeyword = "%" + search.trim().toLowerCase() + "%";

				Predicate nameLike = cb.like(cb.lower(root.get("name")), searchKeyword);
				Predicate phoneLike = cb.like(root.get("phone"), searchKeyword);

				// Gom nhóm (name LIKE ? OR phone LIKE ?)
				predicates.add(cb.or(nameLike, phoneLike));
			}

			// 2. Lọc theo trạng thái hoạt động (Nếu Frontend có truyền lên)
			if (isActive != null) {
				predicates.add(cb.equal(root.get("active"), isActive));
			}

			// Có thể mở rộng thêm lọc theo địa chỉ, khoảng ngày tháng... ở đây sau này

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}