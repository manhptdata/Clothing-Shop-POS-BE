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
    private java.time.Instant startDate;
    private java.time.Instant endDate;
    private Integer totalQuantity;
    private Integer usedQuantity;
    private Integer maxUsagePerUser;
    private Boolean isPublic;
    private Integer targetCustomerGroupId;
    private String applyType;

    public static VoucherResponse fromEntity(com.sapo.mock.clothing.entity.Voucher v) {
        if (v == null) return null;
        VoucherResponse r = new VoucherResponse();
        r.setId(v.getId());
        r.setName(v.getName());
        r.setCode(v.getCode());
        r.setDiscountAmount(v.getDiscountAmount());
        r.setDiscountType(v.getDiscountType());
        r.setMaxDiscountAmount(v.getMaxDiscountAmount());
        r.setMinOrderValue(v.getMinOrderValue());
        r.setStatus(v.getStatus());
        r.setStartDate(v.getStartDate());
        r.setEndDate(v.getEndDate());
        r.setTotalQuantity(v.getTotalQuantity());
        r.setUsedQuantity(v.getUsedQuantity());
        r.setMaxUsagePerUser(v.getMaxUsagePerUser());
        r.setIsPublic(v.getIsPublic());
        r.setTargetCustomerGroupId(v.getTargetCustomerGroupId());
        r.setApplyType(v.getApplyType());
        return r;
    }
}
