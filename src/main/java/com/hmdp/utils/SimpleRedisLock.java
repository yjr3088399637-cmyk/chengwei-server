package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String key;
    private final StringRedisTemplate stringredisTemplate;
    private static final String JVM_ID = UUID.randomUUID(true).toString();
    private String threadId;
    
    //创建脚本对象并加载
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = initUnlockScript();
    private static DefaultRedisScript<Long> initUnlockScript() {
        DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
        return UNLOCK_SCRIPT;
    }

    public SimpleRedisLock(String key, StringRedisTemplate redisTemplate) {
        this.key = key;
        this.stringredisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = JVM_ID + Thread.currentThread().getId();
        this.threadId = threadId;
        return Boolean.TRUE.equals
                (stringredisTemplate.opsForValue().setIfAbsent(key, threadId, timeoutSec, TimeUnit.SECONDS));
    }

    @Override
    public void unlock() {
        //执行lua脚本
        stringredisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), threadId);
    }
//    public void unlock() {
//        String value = stringredisTemplate.opsForValue().get(key);
//        if (value != null && value.equals(threadId)) {
//            stringredisTemplate.delete(key);
//        }
//    }
}
