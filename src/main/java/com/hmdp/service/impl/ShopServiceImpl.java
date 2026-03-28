package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.bytecode.analysis.Executor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;

    @Override
    public Result qurryById(Long id) {

        //只解决缓存穿透
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id, Shop.class,this::getById,CACHE_SHOP_TTL_BASE,TimeUnit.SECONDS);

        //逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpireTime(CACHE_SHOP_KEY,LOCK_SHOP_KEY,id, Shop.class,this::getById);

        //互斥锁解决缓存击穿
//        Shop shop = cacheClient
//                .queryWithMutexLock(CACHE_SHOP_KEY,
//                        LOCK_SHOP_KEY,id,
//                        Shop.class,this::getById,
//                        CACHE_SHOP_TTL_BASE+CACHE_SHOP_TTL_RANDOM,TimeUnit.SECONDS);
        if(shop==null){
            return Result.fail("商户不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateShop(Shop shop) {
        if(shop.getId()==null){
            return Result.fail("id为空");
        }
        updateById(shop);

        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }
}
