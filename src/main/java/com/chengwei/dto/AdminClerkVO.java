package com.chengwei.dto;

import lombok.Data;

@Data
public class AdminClerkVO {
    private Long id;
    private Long shopId;
    private String shopName;
    private String username;
    private String name;
    private Integer role;
    private Integer status;
}
