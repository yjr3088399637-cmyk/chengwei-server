package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "首次设置密码请求")
public class SetPasswordDTO {
    @Schema(description = "要设置的新密码", example = "123456")
    private String password;
}
