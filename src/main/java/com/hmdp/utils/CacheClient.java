package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hmdp.dto.Result;
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
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@Slf4j
@Component
public class CacheClient {





    private static final ExecutorService CACHE_REFRESH_EXECUTOR = Executors.newFixedThreadPool(5);

    @Autowired
    StringRedisTemplate stringRedisTemplate;



    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    @SentinelResource(
            value = "queryWithPassThrough",
            blockHandler = "blockPassThrough",
            fallback = "fallbackPassThrough"
    )
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id , Class<R> type, Function<ID,R> dbFallBack,Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1. 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (Objects.equals(json, "")) {
            // 返回一个错误信息
            return null;
        }
        // 4. 不存在，根据id查询数据库
        R r = dbFallBack.apply(id);
        // 5. 不存在，返回错误
        if (r == null) {
            // 将空值写入redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6. 存在，写入redis
        set(key,r,time,unit);
        return r;
    }
    @SentinelResource(
            value = "queryWithLogicalExpireTime",
            blockHandler = "blockLogicalExpire",
            fallback = "fallbackLogicalExpire"
    )
    public <R,ID> R queryWithLogicalExpireTime(String keyPrefix,String lockPrefix,ID id,Class<R> type,Function<ID,R> dbQuery){
        String key = keyPrefix + id;
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //如果不是空白字符串
        if (StrUtil.isBlank(json)) {
            log.info("缓存未命中，ID:{}", id);
            return null;
        }
        //判断是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean(JSONUtil.toJsonStr(redisData.getData()), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //如果未过期
        if(expireTime.isAfter(LocalDateTime.now())){
            log.info("{}:逻辑未过期，返回最新值", Thread.currentThread().getName());
            return r;
        }
        log.info("{}:逻辑过期，尝试加锁", Thread.currentThread().getName());
        String lockKey = lockPrefix + id;
        //如果已过期则尝试加锁
        boolean isLock = tryLock(lockKey);
        if(!isLock){
            //加锁失败则已有线程处理，返回旧值
            log.info("{}:已有线程处理，返回旧值", Thread.currentThread().getName());
            return r;
        }
        //加锁成功则二次判断是否过期
        redisData = JSONUtil.toBean(json, RedisData.class);
        r = JSONUtil.toBean(JSONUtil.toJsonStr(redisData.getData()), type);
        expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            log.info("{}:加锁成功，但二次判断未过期，直接返回", Thread.currentThread().getName());
            return r;
        }
        //给线程池提交任务
        CACHE_REFRESH_EXECUTOR.submit(()->{
            try {
                log.info("{}:正在异步同步新值到redis", Thread.currentThread().getName());
                saveShop2Redis(id,20L,key,dbQuery);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                unlock(lockKey);
            }
        });
        //提交任务之后依旧返回shop
        return r;
    }

    //封装逻辑过期时间（数据预热）
    public <ID,R> void saveShop2Redis(ID id,Long expireSeconds,String key,Function<ID,R> dbQuery){
        R r = dbQuery.apply(id);
        RedisData redisData = new RedisData(r, LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    @SentinelResource(
            value = "queryWithMutexLock",
            blockHandler = "blockMutexLock",
            fallback = "fallbackMutexLock"
    )
    public <ID,R> R queryWithMutexLock(String keyPrefix,String lockKey,ID id,Class<R> type,Function<ID,R> dbQuery,Long expireTime,TimeUnit unit){
        //从redis中查缓存
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(keyPrefix + id.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //如果不是空白字符串
        if (StrUtil.isNotBlank(json)) {
            log.info("缓存命中，ID:{}", id);
            //缓存命中则返回
            return JSONUtil.toBean(json, type);
        }
        //如果是空值字符串
        if (Objects.equals(json, "")) {
            log.info("缓存空值生效id:{}", id);
            return null;
        }
        log.info("缓存未命中，ID:{}", id);

        //创建锁的key，开始解决缓存击穿
        String lock =  lockKey + id;
        boolean isLock = tryLock(lock);

        R r;
        try {
            if(!isLock){
                //没获取到锁则重新查
                log.info("未获取到锁:{}，阻塞等待",keyPrefix);
                Thread.sleep(100);
                return queryWithMutexLock(keyPrefix,lockKey,id,type,dbQuery,expireTime,unit);
            }
            //未命中则查数据库（空字符串）
            r = dbQuery.apply(id);
            //模拟延时
            Thread.sleep(200);
            if (r == null) {
                //缓存空值，防止缓存穿透
                log.info("正在缓存空值，ID:{}~~", id);
                stringRedisTemplate.opsForValue().set(keyPrefix + id.toString(), "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //将查到的数据放入缓存
            stringRedisTemplate.opsForValue()
                    .set(keyPrefix + id, JSONUtil.toJsonStr(r), expireTime, unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lock);
        }
        return r;
    }



    //加锁逻辑
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }
    //释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    //Sentinel处理逻辑

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
        log.error("异常了，直接查库");
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
        log.error("异常了，直接查库");
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
        log.error("异常了，直接查库");
        return dbQuery.apply(id);
    }

}
