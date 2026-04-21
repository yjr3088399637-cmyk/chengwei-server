package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "店员核销订单请求")
public class ClerkVerifyOrderDTO {
    @Schema(description = "订单 ID", example = "38927852699123725")
    private Long orderId;

    @Schema(description = "用户出示的核销码", example = "CWANAKM6BXIQ3")
    private String verifyCode;
}
