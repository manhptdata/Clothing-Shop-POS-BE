package com.sapo.mock.clothing.customer.dto.request.groupcustomer;


import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class MockOrderRequest {
    private Integer customerId;
    private BigDecimal orderAmount;
}