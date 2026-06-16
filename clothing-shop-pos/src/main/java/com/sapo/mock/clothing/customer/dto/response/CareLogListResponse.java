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

    private CustomerInfo customer;
    private UserInfo calledBy;

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
}