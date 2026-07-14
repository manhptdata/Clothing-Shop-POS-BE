package com.sapo.mock.clothing.receipt.DTO;

import com.sapo.mock.clothing.util.constant.StockLogReferenceType;
import com.sapo.mock.clothing.util.constant.StockLogSource;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class StockLogResponse {

    private Integer id;

    // Thông tin variant
    private Integer variantId;
    private String variantSku;
    private String productName;

    // Biến động số lượng
    private int quantityBefore;
    private int quantityChange;   // âm = giảm, dương = tăng
    private int quantityAfter;

    // Nguồn & tham chiếu
    private StockLogSource source;
    private StockLogReferenceType referenceType;
    private Integer referenceId;

    private String note;
    private Instant createdAt;
    
    // Nguoi thao tac
    private String createdByUsername;
}
