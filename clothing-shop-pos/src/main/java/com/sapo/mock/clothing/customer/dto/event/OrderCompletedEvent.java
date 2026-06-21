package com.sapo.mock.clothing.customer.dto.event;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class OrderCompletedEvent {
    private final Integer customerId;
    private final BigDecimal orderAmount;

    public OrderCompletedEvent(Integer customerId, BigDecimal orderAmount) {
        this.customerId = customerId;
        this.orderAmount = orderAmount;
    }
}