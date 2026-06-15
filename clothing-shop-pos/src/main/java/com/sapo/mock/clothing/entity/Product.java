
//package com.sapo.mock.clothing.entity;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//import org.springframework.data.annotation.CreatedBy;
//import org.springframework.data.annotation.LastModifiedBy;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//import jakarta.persistence.CascadeType;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EntityListeners;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.OneToMany;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Table(name = "product")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@EntityListeners(AuditingEntityListener.class)
//public class Product {
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	private Integer id;
//
//	@Column(nullable = false, unique = true, length = 50)
//	private String sku;
//
//	@Column(nullable = false)
//	private String name;
//
//	@Column(length = 100)
//	private String category;
//
//	@Column(length = 50)
//	private String color;
//
//	@Column(length = 20)
//	private String size;
//
//	@Column(name = "sale_price", nullable = false, precision = 15, scale = 2)
//	private BigDecimal salePrice;
//
//	@Column(name = "import_price", precision = 15, scale = 2)
//	private BigDecimal importPrice;
//
//	private String description;
//
//	@JdbcTypeCode(SqlTypes.JSON)
//	@Column(name = "image_urls")
//	private List<String> imageUrls;
//
//	@Builder.Default
//	@Column(name = "low_stock_threshold", nullable = false)
//	private Integer lowStockThreshold = 5;
//
//	@Builder.Default
//	@Column(name = "is_deleted", nullable = false)
//	private Boolean isDeleted = false;
//
//	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
//	private List<ProductVariant> variants = new ArrayList<>();
//
//	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
//	private List<ProductAttribute> attributes;
//
//	public void addAttribute(ProductAttribute attr) {
//		attributes.add(attr);
//		attr.setProduct(this);
//	}
//
//	public void removeAttribute(ProductAttribute attr) {
//		attributes.remove(attr);
//		attr.setProduct(null);
//	}
//
//	public void addVariant(ProductVariant variant) {
//		variants.add(variant);
//		variant.setProduct(this);
//	}
//
//	public void removeVariant(ProductVariant variant) {
//		variants.remove(variant);
//		variant.setProduct(null);
//	}
//
//	@CreationTimestamp
//	@Column(name = "created_at", nullable = false, updatable = false)
//	private LocalDateTime createdAt;
//
//	@org.hibernate.annotations.UpdateTimestamp
//	@Column(name = "updated_at")
//	private LocalDateTime updatedAt;
//
//	@LastModifiedBy
//	@Column(name = "updated_by")
//	private Integer updatedBy;
//
//	@CreatedBy
//	@Column(name = "created_by")
//	private Integer createdBy;
//
//
//
//
//}

package com.sapo.mock.clothing.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String name;

	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "image_urls")
	private List<String> imageUrls;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductAttribute> attributes = new ArrayList<>();

	// THÊM MỚI: Danh sách biến thể (Variant)
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductVariant> variants = new ArrayList<>();

	public void addAttribute(ProductAttribute attr) {
		attributes.add(attr);
		attr.setProduct(this);
	}

	public void removeAttribute(ProductAttribute attr) {
		attributes.remove(attr);
		attr.setProduct(null);
	}

	// THÊM MỚI: Helper method cho Variant
	public void addVariant(ProductVariant variant) {
		variants.add(variant);
		variant.setProduct(this);
	}

	public void removeVariant(ProductVariant variant) {
		variants.remove(variant);
		variant.setProduct(null);
	}

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@org.hibernate.annotations.UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@LastModifiedBy
	@Column(name = "updated_by")
	private Integer updatedBy;

	@CreatedBy
	@Column(name = "created_by")
	private Integer createdBy;
}