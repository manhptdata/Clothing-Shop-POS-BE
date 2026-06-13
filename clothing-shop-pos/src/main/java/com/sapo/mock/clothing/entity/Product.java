
package com.sapo.mock.clothing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	@Column(nullable = false, unique = true, length = 50)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column(length = 100)
	private String category;

	@Column(length = 50)
	private String color;

	@Column(length = 20)
	private String size;

	@Column(name = "sale_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "import_price", precision = 15, scale = 2)
	private BigDecimal importPrice;

	private String description;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "image_urls")
	private List<String> imageUrls;

	@Builder.Default
	@Column(name = "low_stock_threshold", nullable = false)
	private Integer lowStockThreshold = 5;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductAttribute> attributes;

	public void addAttribute(ProductAttribute attr) {
		attributes.add(attr);
		attr.setProduct(this);
	}

	public void removeAttribute(ProductAttribute attr) {
		attributes.remove(attr);
		attr.setProduct(null);
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

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "updated_by")
//	private User updatedBy;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "created_by")
//	private User createdBy;

//	@PrePersist
//	public void prePersist() {
//		this.createdAt = Instant.now();
//		this.updatedAt = Instant.now();
//	}
//
//	@PreUpdate
//	public void preUpdate() {
//		this.updatedAt = Instant.now();
//	}
}