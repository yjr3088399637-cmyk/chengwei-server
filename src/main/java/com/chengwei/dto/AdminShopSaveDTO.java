package com.chengwei.dto;

import lombok.Data;

@Data
public class AdminShopSaveDTO {
    private String name;
    private Long typeId;
    private String images;
    private String area;
    private String address;
    private Double x;
    private Double y;
    private Long avgPrice;
    private String openHours;
    private String clerkUsername;
    private String clerkPassword;
    private String clerkName;
}
