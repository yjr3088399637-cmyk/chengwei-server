package com.chengwei.dto;

import lombok.Data;

@Data
public class ClerkShopUpdateDTO {
    private String name;
    private String images;
    private String area;
    private String address;
    private Long avgPrice;
    private String openHours;
}
