package com.sapo.mock.clothing.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVoucherHistoryResponse {
    private Integer id;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String voucherName;
    private String voucherCode;
    private Instant receivedAt;
    private Instant expiredAt;
    private Instant usedAt;
    private String status;
}
