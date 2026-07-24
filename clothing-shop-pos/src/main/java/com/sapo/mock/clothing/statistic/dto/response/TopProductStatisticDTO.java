package com.sapo.mock.clothing.statistic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductStatisticDTO {
    private String productName;
    private String productSku;
    private long soldQuantity;
    private BigDecimal totalRevenue;
}
