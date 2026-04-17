package com.chengwei.dto;

import lombok.Data;

@Data
public class ClerkVerifyOrderDTO {
    private Long orderId;
    private String verifyCode;
}
