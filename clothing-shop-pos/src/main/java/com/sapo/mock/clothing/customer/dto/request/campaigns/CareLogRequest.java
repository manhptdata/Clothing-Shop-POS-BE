package com.sapo.mock.clothing.customer.dto.request.campaigns;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class CareLogRequest {

    @NotNull(message = "ID khách hàng không được để trống")
    private Integer customerId;

    @NotNull(message = "Kết quả cuộc gọi không được để trống")
    private String result;      // Ví dụ: "NGHE_MAY", "CUOC_GOI_NHO", "HEN_GOI_LAI", "TU_CHOI"

    private String note;        // Chi tiết cuộc trao đổi (Gõ tay)

    private Instant nextRetryAt; // Lịch hẹn gọi lại lần sau (nếu có)
}