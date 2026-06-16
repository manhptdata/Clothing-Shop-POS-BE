package com.sapo.mock.clothing.customer.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class CareLogResponse {
    private Integer id;
    private String result;
    private String note;
    private Instant scheduledAt;
    private Instant calledAt;
    private Instant nextRetryAt;
    private Instant createdAt;

    private CustomerInfo customer;
    private CampaignInfo campaign;
    private UserInfo calledBy;
    private OrderInfo order;

    @Getter
    @Setter
    public static class CustomerInfo {
        private Integer id;
        private String fullName;
        private String phone;
    }

    @Getter
    @Setter
    public static class CampaignInfo {
        private Integer id;
        private String name;
    }

    @Getter
    @Setter
    public static class UserInfo {
        private Integer id;
        private String username;
        private String fullName; // Tên nhân viên gọi điện
    }

    @Getter
    @Setter
    public static class OrderInfo {
        private Integer id;
        private String orderNumber;
    }
}