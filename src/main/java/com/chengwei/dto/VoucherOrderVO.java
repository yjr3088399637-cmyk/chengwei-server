package com.chengwei.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherOrderVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long voucherId;
    private Long shopId;
    private String shopName;
    private String userNickName;
    private String userPhone;
    private String verifyCode;
    private String verifyClerkName;
    private String voucherTitle;
    private String voucherSubTitle;
    private Long payValue;
    private Long actualValue;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime useTime;
}
