package com.sapo.mock.clothing.statistic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticItemResponse {
    private LocalDate date;
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal profit;
    private long orderCount;
}
