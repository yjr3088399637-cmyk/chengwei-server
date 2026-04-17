package com.chengwei.dto;

import lombok.Data;

@Data
public class ClerkStaffVO {
    private Long id;
    private Long shopId;
    private String username;
    private String name;
    private Integer role;
    private Integer status;
}
