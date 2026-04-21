package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "管理端首页统计概览")
public class AdminOverviewDTO {
    @Schema(description = "店铺总数", example = "18")
    private Long shopCount;

    @Schema(description = "店员账号总数", example = "28")
    private Long clerkCount;

    @Schema(description = "当前可售优惠券数量", example = "2")
    private Long activeVoucherCount;

    @Schema(description = "有效店铺数量", example = "18")
    private Long onlineShopCount;
}
