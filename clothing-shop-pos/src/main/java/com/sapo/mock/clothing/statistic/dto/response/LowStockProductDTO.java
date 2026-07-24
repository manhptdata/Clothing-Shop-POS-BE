package com.sapo.mock.clothing.statistic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockProductDTO {
    private Integer variantId;
    private String productName;
    private String sku;
    private int currentQuantity;
    private int lowStockThreshold;
}
