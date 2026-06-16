package com.sapo.mock.clothing.customer.dto.response;

import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.GenderEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

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


    // Hiện tại chưa làm tính năng điểm thì gán mặc định luôn ở đây
  /*  private Integer rewardPoints = 0;
    private String membershipRank = "MỚI";*/

    // Object con trả về thông tin nhóm rút gọn (chỉ gồm ID và Tên nhóm)
    private GroupInfo customerGroup;



    @Getter
    @Setter
    public static class GroupInfo {
        private Integer id;
        private String name;
    }


}