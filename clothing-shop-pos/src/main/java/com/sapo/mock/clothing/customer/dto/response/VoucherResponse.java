package com.sapo.mock.clothing.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private Integer id;
    private String name;
    private String code;
    private BigDecimal discountAmount;
    private com.sapo.mock.clothing.util.constant.VoucherDiscountType discountType;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum status;
}
