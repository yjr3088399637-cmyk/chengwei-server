package com.chengwei.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.Result;
import com.chengwei.entity.Shop;
import com.chengwei.mapper.ShopMapper;
import com.chengwei.service.IShopService;
import com.chengwei.utils.cache.CacheClient;
import com.chengwei.utils.redis.BloomFilter;
import com.chengwei.utils.redis.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.chengwei.utils.redis.RedisConstants.CACHE_SHOP_KEY;
import static com.chengwei.utils.redis.RedisConstants.LOCK_SHOP_KEY;
import static com.chengwei.utils.redis.RedisConstants.SHOP_GEO_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    private final RedissonClient redissonClient;
    private final BloomFilter bloomFilter;

    //初始化过滤器
    @PostConstruct
    public void initBloomFilter() {
        bloomFilter.initBloomFilter(RedisConstants.SHOP_BLOOM_KEY,this,Shop::getId);
    }

    @Override
    public Result qurryById(Long id) {
        //布隆过滤器校验
        Boolean contain = bloomFilter.filter(RedisConstants.SHOP_BLOOM_KEY, id);
        if (!contain) {
            return Result.fail("非法id:"+ id);
        }

        Shop shop = cacheClient.queryWithLogicalExpireTime(
                CACHE_SHOP_KEY,
                LOCK_SHOP_KEY,
                id,
                Shop.class,
                this::getById
        );
        if (shop == null) {
            return Result.fail("商户不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateShop(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, String sortBy, Double x, Double y) {
        if (StrUtil.isBlank(sortBy)) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .orderByAsc("id")
                    .page(new Page<>(current, 5));
            return Result.ok(page.getRecords());
        }

        if ("comments".equals(sortBy) || "score".equals(sortBy)) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .orderByDesc(sortBy)
                    .orderByDesc("id")
                    .page(new Page<>(current, 5));
            return Result.ok(page.getRecords());
        }

        if (!"distance".equals(sortBy) || x == null || y == null) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .orderByAsc("id")
                    .page(new Page<>(current, 5));
            return Result.ok(page.getRecords());
        }

        int pageSize = 5;
        int from = (current - 1) * pageSize;
        int end = current * pageSize;

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(
                SHOP_GEO_KEY + typeId,
                new Circle(new Point(x, y), new Distance(5000)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end)
        );
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (content.size() <= from) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(content.size() - from);
        Map<String, Distance> distanceMap = new HashMap<>(content.size() - from);
        content.stream().skip(from).forEach(result -> {
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            distanceMap.put(shopIdStr, result.getDistance());
        });

        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids)
                .last("order by field(id," + idStr + ")")
                .list();
        shops.forEach(shop -> {
            Distance distance = distanceMap.get(shop.getId().toString());
            if (distance != null) {
                shop.setDistance(distance.getValue());
            }
        });
        return Result.ok(shops);
    }
}
