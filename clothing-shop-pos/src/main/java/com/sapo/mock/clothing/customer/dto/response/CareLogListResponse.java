package com.sapo.mock.clothing.customer.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class CareLogListResponse {
    private Integer id;
    private String result;
    private Instant calledAt;
    private Instant createdAt;
    private String potentialStatus;

    private CustomerInfo customer;
    private UserInfo calledBy;
    private CampaignInfo campaign;

    @Getter
    @Setter
    public static class CustomerInfo {
        private Integer id;
        private String fullName;
        private String phone;
    }

    @Getter
    @Setter
    public static class UserInfo {
        private Integer id;
        private String username;
        private String fullName;
    }

    @Getter
    @Setter
    public static class CampaignInfo {
        private Integer id;
        private String name;
        private String type;
    }
}