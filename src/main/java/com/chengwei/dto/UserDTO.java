package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户简要信息")
public class UserDTO {
    @Schema(description = "用户 ID", example = "1010")
    private Long id;

    @Schema(description = "昵称", example = "小可爱")
    private String nickName;

    @Schema(description = "头像链接", example = "https://example.com/avatar.jpg")
    private String icon;
}
