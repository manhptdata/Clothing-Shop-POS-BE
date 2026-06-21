package com.sapo.mock.clothing.customer.dto.response;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerGroupResponse {
    private Integer id;
    private String name;
    private String description;
    private CustomerStatusEnum status;
    private Long totalCustomers;
    private String note;
    private BigDecimal minSpending;
    private BigDecimal maxSpending;
    private RankCodeEnum code;
    private Instant createdAt;

    public CustomerGroupResponse(Integer id, String name, String description, CustomerStatusEnum status, Long totalCustomers, String note, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.totalCustomers = totalCustomers;
        this.note = note;
        this.createdAt = createdAt;
    }
}