package com.sapo.mock.clothing.statistic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticResponse {
    private BigDecimal dailyRevenue;
    private BigDecimal dailyCogs;
    private BigDecimal dailyProfit;
    private long newCustomers;
    private long newOrders;
}
