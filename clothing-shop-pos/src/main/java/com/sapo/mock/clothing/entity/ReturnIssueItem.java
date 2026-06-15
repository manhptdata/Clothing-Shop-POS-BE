package com.sapo.mock.clothing.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "return_issue_item")
public class ReturnIssueItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "return_ticket_id", nullable = false)
	private ReturnTicket returnTicket;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id", nullable = false)
	private ProductVariant variant;

	@Column(name = "product_name", nullable = false, length = 255)
	private String productName;

	@Column(name = "product_sku", nullable = false, length = 50)
	private String productSku;

	@Column(nullable = false)
	private int quantity = 1;

	@Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal unitPrice;

	// STORED computed column — chỉ đọc, không ghi từ Java
	@Column(nullable = false, precision = 15, scale = 2, insertable = false, updatable = false)
	private BigDecimal subtotal;
}
