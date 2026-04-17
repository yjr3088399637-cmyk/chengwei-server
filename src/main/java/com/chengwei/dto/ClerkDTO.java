package com.chengwei.dto;

import lombok.Data;

@Data
public class ClerkDTO {
    private Long id;
    private Long shopId;
    private String username;
    private String name;
    private Integer role;
}
