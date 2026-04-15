package com.chengwei.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherOrderVO {
    private Long id;
    private Long voucherId;
    private Long shopId;
    private String shopName;
    private String voucherTitle;
    private String voucherSubTitle;
    private Long payValue;
    private Long actualValue;
    private Integer status;
    private LocalDateTime createTime;
}
