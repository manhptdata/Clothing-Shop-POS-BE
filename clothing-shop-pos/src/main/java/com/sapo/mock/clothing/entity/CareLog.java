package com.sapo.mock.clothing.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_log")
public class CareLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "campaign_id")
	private CareCampaign campaign;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY)

	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "called_by", nullable = false)
	private User calledBy;

	@Column(length = 20)
	private String result;

	@Column(columnDefinition = "TEXT")
	private String note;

	@Column(name = "scheduled_at")
	private Instant scheduledAt;

	@Column(name = "called_at")
	private Instant calledAt;

	@Column(name = "next_retry_at")
	private Instant nextRetryAt;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = Instant.now();
	}
}
