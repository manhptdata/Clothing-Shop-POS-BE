package com.sapo.mock.clothing.specification;

import org.springframework.data.jpa.domain.Specification;

import com.sapo.mock.clothing.entity.User;

public class UserSpecification {
	// Lọc tìm kiếm theo 1 trong 3 trường
	public static Specification<User> build(String search, Boolean active) {

		return (root, query, cb) -> {
			if (search == null || search.isEmpty()) {
				return null;
			}
			String likePattern = "%" + search.toLowerCase() + "%";

			// Tìm theo username HOẶC fullName HOẶC phone
			return cb.or(cb.like(cb.lower(root.get("username")), likePattern),
					cb.like(cb.lower(root.get("fullName")), likePattern), cb.like(root.get("phone"), likePattern));
		};
	}

	// Lọc theo trạng thái active
	public static Specification<User> isActive(Boolean active) {
		return (root, query, cb) -> (active == null) ? null : cb.equal(root.get("active"), active);
	}
}
