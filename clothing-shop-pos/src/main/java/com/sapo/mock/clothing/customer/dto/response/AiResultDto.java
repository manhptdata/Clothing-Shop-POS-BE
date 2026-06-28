package com.sapo.mock.clothing.customer.dto.response;


import lombok.Data;

@Data
public class AiResultDto {
    private String result;
    private String potentialStatus;
    private String nextRetryTime;
}
