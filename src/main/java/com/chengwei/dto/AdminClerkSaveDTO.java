package com.chengwei.dto;

import lombok.Data;

@Data
public class AdminClerkSaveDTO {
    private Long shopId;
    private String username;
    private String password;
    private String name;
}
