
package com.sapo.mock.clothing.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sapo.mock.clothing.entity.Product;
import com.sapo.mock.clothing.entity.ProductVariant; // Import Entity mới

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class ProductSpecification {
	public static Specification<Product> build(String search, String productName, String sku, Integer categoryId,
			Boolean isDeleted, String stockStatus) {

		return (root, query, cb) -> {
			// QUAN TRỌNG: Loại bỏ các kết quả lặp lại (duplicate) khi dùng phép JOIN
			// OneToMany
			query.distinct(true);

			List<Predicate> predicates = new ArrayList<>();

			// Khởi tạo Join (Chỉ Join 1 lần duy nhất nếu có điều kiện tìm kiếm liên quan
			// đến Variant)
			Join<Product, ProductVariant> variantJoin = null;
			boolean needVariantJoin = (search != null && !search.trim().isEmpty()) || (sku != null && !sku.isBlank());

			if (needVariantJoin) {
				// Dùng LEFT JOIN để tránh làm rớt các Product chưa kịp khởi tạo Variant
				variantJoin = root.join("variants", JoinType.LEFT);
			}

			// 1. Lọc theo chuỗi tìm kiếm chung (Tên Sản phẩm HOẶC Sku Biến thể)
			if (search != null && !search.trim().isEmpty()) {
				String pattern = "%" + search.trim().toLowerCase() + "%";

				Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
				Predicate skuLike = cb.like(cb.lower(variantJoin.get("sku")), pattern); // Truy xuất sku từ bảng Join

				predicates.add(cb.or(nameLike, skuLike));
			}

			// 2. Lọc chính xác theo tên sản phẩm
			if (productName != null && !productName.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + productName.toLowerCase().trim() + "%"));
			}

			// 3. Lọc chính xác theo SKU
			if (sku != null && !sku.isBlank()) {
				predicates.add(cb.like(cb.lower(variantJoin.get("sku")), "%" + sku.toLowerCase().trim() + "%"));
			}

			// 4. Lọc theo Category (Bạn khai báo trên tham số nhưng chưa dùng trong hàm cũ)
			if (categoryId != null) {
				predicates.add(cb.equal(root.get("category").get("id"), categoryId));
			}

			// 5. Lọc trạng thái xóa
			if (isDeleted != null) {
				predicates.add(cb.equal(root.get("isDeleted"), isDeleted));
			}

//			6. LỌC THEO TRẠNG THÁI TỒN KHO (DỰA TRÊN lowStockThreshold)
			if (stockStatus != null && !stockStatus.isEmpty()) {
				// Tạo Subquery để kiểm tra trạng thái của các variants
				Subquery<Long> subquery = query.subquery(Long.class);
				Root<ProductVariant> variantRoot = subquery.from(ProductVariant.class);

				// Đếm số lượng variants thỏa mãn điều kiện
				subquery.select(cb.count(variantRoot)).where(cb.equal(variantRoot.get("product"), root));

				switch (stockStatus) {
				case "in-stock":
					// Còn hàng: Có ít nhất 1 variant có quantity > lowStockThreshold
					Subquery<Long> inStockSubquery = query.subquery(Long.class);
					Root<ProductVariant> inStockRoot = inStockSubquery.from(ProductVariant.class);
					inStockSubquery.select(cb.count(inStockRoot)).where(cb.equal(inStockRoot.get("product"), root),
							cb.greaterThan(inStockRoot.get("quantity"), inStockRoot.get("lowStockThreshold")));
					predicates.add(cb.greaterThan(inStockSubquery, 0L));
					break;

				case "low-stock":
					// Sắp hết:
					// 1. KHÔNG có variant nào có quantity > lowStockThreshold
					// 2. Có ít nhất 1 variant có 0 < quantity <= lowStockThreshold

					// Subquery đếm variant có quantity > lowStockThreshold
					Subquery<Long> hasStockSubquery = query.subquery(Long.class);
					Root<ProductVariant> hasStockRoot = hasStockSubquery.from(ProductVariant.class);
					hasStockSubquery.select(cb.count(hasStockRoot)).where(cb.equal(hasStockRoot.get("product"), root),
							cb.greaterThan(hasStockRoot.get("quantity"), hasStockRoot.get("lowStockThreshold")));

					// Subquery đếm variant có 0 < quantity <= lowStockThreshold
					Subquery<Long> lowStockSubquery = query.subquery(Long.class);
					Root<ProductVariant> lowStockRoot = lowStockSubquery.from(ProductVariant.class);
					lowStockSubquery.select(cb.count(lowStockRoot)).where(cb.equal(lowStockRoot.get("product"), root),
							cb.greaterThan(lowStockRoot.get("quantity"), 0L),
							cb.lessThanOrEqualTo(lowStockRoot.get("quantity"), lowStockRoot.get("lowStockThreshold")));

					// Điều kiện: không có variant nào còn hàng (quantity > threshold)
					// VÀ có ít nhất 1 variant sắp hết
					predicates.add(cb.equal(hasStockSubquery, 0L));
					predicates.add(cb.greaterThan(lowStockSubquery, 0L));
					break;

				case "out-of-stock":
					// Hết hàng: Tất cả variants đều có quantity = 0
					// Tức là không có variant nào có quantity > 0
					Subquery<Long> outOfStockSubquery = query.subquery(Long.class);
					Root<ProductVariant> outOfStockRoot = outOfStockSubquery.from(ProductVariant.class);
					outOfStockSubquery.select(cb.count(outOfStockRoot)).where(
							cb.equal(outOfStockRoot.get("product"), root),
							cb.greaterThan(outOfStockRoot.get("quantity"), 0L));
					predicates.add(cb.equal(outOfStockSubquery, 0L));
					break;
				case "partial-out-of-stock":
					// Yêu cầu mới: Có ít nhất 1 variant có quantity <= 0
					Subquery<Long> partialOutOfStockSubquery = query.subquery(Long.class);
					Root<ProductVariant> partialOutOfStockkRoot = partialOutOfStockSubquery.from(ProductVariant.class);

					// Đếm số lượng variant của sản phẩm này có quantity <= 0
					partialOutOfStockSubquery.select(cb.count(partialOutOfStockkRoot)).where(
							cb.equal(partialOutOfStockkRoot.get("product"), root),
							cb.lessThanOrEqualTo(partialOutOfStockkRoot.get("quantity"), 0));

					// Điều kiện: Số lượng variant hết hàng phải > 0 (tức là tồn tại ít nhất 1 cái)
					predicates.add(cb.greaterThan(partialOutOfStockSubquery, 0L));
					break;

				default:
					break;
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}