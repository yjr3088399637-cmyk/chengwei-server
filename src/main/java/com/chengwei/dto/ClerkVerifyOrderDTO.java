package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static com.chengwei.utils.common.RegexPatterns.VERIFY_CODE_REGEX;

@Data
@Schema(description = "店员核销订单请求")
public class ClerkVerifyOrderDTO {
    @Schema(description = "订单 ID", example = "38927852699123725")
    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    @Schema(description = "用户出示的核销码", example = "CWANAKM6BXIQ3")
    @NotBlank(message = "核销码不能为空")
    @Pattern(regexp = VERIFY_CODE_REGEX, message = "核销码格式错误")
    private String verifyCode;
}
