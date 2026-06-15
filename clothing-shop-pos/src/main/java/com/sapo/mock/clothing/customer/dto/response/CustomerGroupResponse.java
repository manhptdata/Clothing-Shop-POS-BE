package com.sapo.mock.clothing.customer.dto.response;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private Instant createdAt;

}