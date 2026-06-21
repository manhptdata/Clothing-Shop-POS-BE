package com.sapo.mock.clothing.customer.dto.response;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum;
import com.sapo.mock.clothing.util.constant.GenderEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class CustomerResponse {
    private Integer id;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private GenderEnum gender;
    private String address;
    private String note;
    private CustomerStatusEnum status;
    private Instant createdAt;
    private Integer rewardPoints;


    // Hiện tại chưa làm tính năng điểm thì gán mặc định luôn ở đây
   /* private Integer rewardPoints = 0;
    private String membershipRank = "MỚI";*/

    // Object con trả về thông tin nhóm rút gọn (chỉ gồm ID và Tên nhóm)
    private GroupInfo customerGroup;

    // Danh sách voucher của khách hàng (chỉ có khi gọi API chi tiết GET /{id})
    private List<VoucherInfo> vouchers;

    @Getter
    @Setter
    public static class GroupInfo {
        private Integer id;
        private String name;
        private RankCodeEnum code;
    }

    @Getter
    @Setter
    public static class VoucherInfo {
        private Integer id;                      // ID bản ghi customer_voucher
        private String voucherCode;              // Mã voucher (VD: BDAY-2026-034)
        private String voucherName;              // Tên voucher
        private BigDecimal discountAmount;       // Số tiền giảm
        private BigDecimal minOrderValue;        // Đơn tối thiểu để áp dụng
        private CustomerVoucherStatusEnum status; // UNUSED / USED / EXPIRED
        private Instant receivedAt;             // Ngày nhận
        private Instant expiredAt;              // Hạn sử dụng
        private Instant usedAt;                // Ngày đã dùng (null nếu chưa dùng)
    }

}