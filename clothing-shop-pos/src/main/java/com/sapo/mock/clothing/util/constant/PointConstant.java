package com.sapo.mock.clothing.util.constant;

import java.math.BigDecimal;

public class PointConstant {
    // 1.000 VNĐ chi tiêu = 1 Điểm
    public static final BigDecimal EARN_RATE = new BigDecimal("1000"); 

    // 1 Điểm = 1.000 VNĐ giảm giá
    public static final BigDecimal REDEEM_RATE = new BigDecimal("1000"); 

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_REDEEM = "REDEEM";
    public static final String TYPE_REFUND = "REFUND";
}
