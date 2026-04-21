package com.chengwei.utils.redis;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;

    public RedisData(Object data, LocalDateTime expireTime) {
        this.data = data;
        this.expireTime = expireTime;
    }
    public RedisData(){

    }
}
