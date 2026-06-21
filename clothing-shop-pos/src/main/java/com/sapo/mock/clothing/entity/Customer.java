package com.sapo.mock.clothing.entity;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.GenderEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /*@Column(length = 20)
    private String gender;*/
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GenderEnum gender;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String note;

    /* @Column(length = 20)
     private String status = "ACTIVE";*/
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CustomerStatusEnum status = CustomerStatusEnum.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_group_id")
    private CustomerGroup customerGroup;


    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private BigDecimal totalSpent; // Tổng chi tiêu của khách hàng
    
    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints = 0; // Điểm thưởng tích lũy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
