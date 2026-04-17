package com.chengwei.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminOverviewDTO {
    private Long shopCount;
    private Long clerkCount;
    private Long activeVoucherCount;
    private Long onlineShopCount;
}
