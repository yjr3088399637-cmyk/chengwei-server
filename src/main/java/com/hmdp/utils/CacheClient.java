package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;

@Slf4j
@Component
public class CacheClient {

    private static final ExecutorService CACHE_REFRESH_EXECUTOR = Executors.newFixedThreadPool(5);
    private static final long DEFAULT_LOGICAL_EXPIRE_SECONDS = 20L;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    @SentinelResource(
            value = "queryWithPassThrough",
            blockHandler = "blockPassThrough",
            fallback = "fallbackPassThrough"
    )
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallBack, Long time, TimeUnit unit
    ) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (Objects.equals(json, "")) {
            return null;
        }
        R r = dbFallBack.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        set(key, r, time, unit);
        return r;
    }

    @SentinelResource(
            value = "queryWithLogicalExpireTime",
            blockHandler = "blockLogicalExpire",
            fallback = "fallbackLogicalExpire"
    )
    public <R, ID> R queryWithLogicalExpireTime(
            String keyPrefix, String lockPrefix, ID id, Class<R> type, Function<ID, R> dbQuery
    ) {
        String key = keyPrefix + id;
        String json = getCacheValue(key);
        if (StrUtil.isBlank(json)) {
            log.info("缓存未命中，ID:{}", id);
            return queryWithMutexLockAndLogicalExpire(
                    keyPrefix, lockPrefix, id, type, dbQuery, DEFAULT_LOGICAL_EXPIRE_SECONDS, TimeUnit.SECONDS
            );
        }

        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean(JSONUtil.toJsonStr(redisData.getData()), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime != null && expireTime.isAfter(LocalDateTime.now())) {
            log.info("{}:逻辑未过期，直接返回缓存", Thread.currentThread().getName());
            return r;
        }

        log.info("{}:逻辑已过期，尝试获取锁重建缓存", Thread.currentThread().getName());
        String lockKey = lockPrefix + id;
        boolean isLock = tryLock(lockKey);
        if (!isLock) {
            log.info("{}:已有线程在重建缓存，先返回旧值", Thread.currentThread().getName());
            return r;
        }

        String latestJson = getCacheValue(key);
        if (StrUtil.isBlank(latestJson)) {
            unlock(lockKey);
            return queryWithMutexLockAndLogicalExpire(
                    keyPrefix, lockPrefix, id, type, dbQuery, DEFAULT_LOGICAL_EXPIRE_SECONDS, TimeUnit.SECONDS
            );
        }

        RedisData latestRedisData = JSONUtil.toBean(latestJson, RedisData.class);
        R latestData = JSONUtil.toBean(JSONUtil.toJsonStr(latestRedisData.getData()), type);
        LocalDateTime latestExpireTime = latestRedisData.getExpireTime();
        if (latestExpireTime != null && latestExpireTime.isAfter(LocalDateTime.now())) {
            unlock(lockKey);
            log.info("{}:拿到锁后发现缓存已刷新，直接返回", Thread.currentThread().getName());
            return latestData;
        }

        CACHE_REFRESH_EXECUTOR.submit(() -> {
            try {
                log.info("{}:异步重建逻辑过期缓存", Thread.currentThread().getName());
                saveShop2Redis(id, DEFAULT_LOGICAL_EXPIRE_SECONDS, key, dbQuery);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                unlock(lockKey);
            }
        });
        return latestData;
    }

    public <ID, R> void saveShop2Redis(ID id, Long expireSeconds, String key, Function<ID, R> dbQuery) {
        R r = dbQuery.apply(id);
        RedisData redisData = new RedisData(r, LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    @SentinelResource(
            value = "queryWithMutexLock",
            blockHandler = "blockMutexLock",
            fallback = "fallbackMutexLock"
    )
    public <ID, R> R queryWithMutexLock(
            String keyPrefix, String lockKey, ID id, Class<R> type,
            Function<ID, R> dbQuery, Long expireTime, TimeUnit unit
    ) {
        String json = getCacheValue(keyPrefix + id);
        if (StrUtil.isNotBlank(json)) {
            log.info("缓存命中，ID:{}", id);
            return JSONUtil.toBean(json, type);
        }
        if (Objects.equals(json, "")) {
            log.info("缓存空值命中，ID:{}", id);
            return null;
        }
        log.info("缓存未命中，ID:{}", id);

        String lock = lockKey + id;
        boolean isLock = tryLock(lock);

        R r;
        try {
            if (!isLock) {
                log.info("未获取到锁{}，阻塞等待", keyPrefix);
                Thread.sleep(100);
                return queryWithMutexLock(keyPrefix, lockKey, id, type, dbQuery, expireTime, unit);
            }
            r = dbQuery.apply(id);
            Thread.sleep(200);
            if (r == null) {
                log.info("正在缓存空值，ID:{}~~", id);
                stringRedisTemplate.opsForValue().set(keyPrefix + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(keyPrefix + id, JSONUtil.toJsonStr(r), expireTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (isLock) {
                unlock(lock);
            }
        }
        return r;
    }

    public <ID, R> R queryWithMutexLockAndLogicalExpire(
            String keyPrefix, String lockKeyPrefix, ID id, Class<R> type,
            Function<ID, R> dbQuery, Long expireTime, TimeUnit unit
    ) {
        String key = keyPrefix + id;
        String json = getCacheValue(key);
        if (StrUtil.isNotBlank(json)) {
            RedisData redisData = JSONUtil.toBean(json, RedisData.class);
            return JSONUtil.toBean(JSONUtil.toJsonStr(redisData.getData()), type);
        }
        if (Objects.equals(json, "")) {
            log.info("缓存空值命中，ID:{}", id);
            return null;
        }
        log.info("缓存未命中，ID:{}，进入互斥锁重建", id);

        String lockKey = lockKeyPrefix + id;
        boolean isLock = tryLock(lockKey);
        try {
            if (!isLock) {
                log.info("未获取到锁{}，阻塞等待", lockKey);
                Thread.sleep(100);
                return queryWithMutexLockAndLogicalExpire(keyPrefix, lockKeyPrefix, id, type, dbQuery, expireTime, unit);
            }

            json = getCacheValue(key);
            if (StrUtil.isNotBlank(json)) {
                RedisData redisData = JSONUtil.toBean(json, RedisData.class);
                return JSONUtil.toBean(JSONUtil.toJsonStr(redisData.getData()), type);
            }
            if (Objects.equals(json, "")) {
                return null;
            }

            R r = dbQuery.apply(id);
            Thread.sleep(200);
            if (r == null) {
                log.info("正在缓存空值，ID:{}~~", id);
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            setWithLogicalExpire(key, r, expireTime, unit);
            return r;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (isLock) {
                unlock(lockKey);
            }
        }
    }

    private String getCacheValue(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    public <R, ID> R blockPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallBack, Long time, TimeUnit unit,
            BlockException e
    ) {
        log.error("限流了:{}", e.getMessage());
        return null;
    }

    public <R, ID> R fallbackPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallBack, Long time, TimeUnit unit,
            Throwable ex
    ) {
        log.error("发生异常，直接查库");
        return dbFallBack.apply(id);
    }

    public <R, ID> R blockLogicalExpire(
            String keyPrefix, String lockPrefix, ID id, Class<R> type, Function<ID, R> dbQuery,
            BlockException e
    ) {
        log.error("限流了:{}", e.getMessage());
        return null;
    }

    public <R, ID> R fallbackLogicalExpire(
            String keyPrefix, String lockPrefix, ID id, Class<R> type, Function<ID, R> dbQuery,
            Throwable ex
    ) {
        log.error("发生异常，直接查库");
        return dbQuery.apply(id);
    }

    public <R, ID> R blockMutexLock(
            String keyPrefix, String lockKey, ID id, Class<R> type,
            Function<ID, R> dbQuery, Long expireTime, TimeUnit unit,
            BlockException e
    ) {
        log.error("限流了:{}", e.getMessage());
        return null;
    }

    public <R, ID> R fallbackMutexLock(
            String keyPrefix, String lockKey, ID id, Class<R> type,
            Function<ID, R> dbQuery, Long expireTime, TimeUnit unit,
            Throwable ex
    ) {
        log.error("发生异常，直接查库");
        return dbQuery.apply(id);
    }
}
