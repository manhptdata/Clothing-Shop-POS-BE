package com.sapo.mock.clothing.statistic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticResponse {
    private BigDecimal dailyRevenue;
    private BigDecimal dailyCogs;
    private BigDecimal dailyProfit;
    private long newCustomers;
    private long newOrders;

    private BigDecimal averageOrderValue;
    private List<TopProductStatisticDTO> topProducts;
    private List<LowStockProductDTO> lowStockProducts;
    private Map<String, BigDecimal> paymentMethodBreakdown;
}
