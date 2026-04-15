package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


@SpringBootTest
class ChengWeiApplicationTests {
    @Autowired
    ShopServiceImpl shopService;
    @Autowired
    CacheClient cacheClient;
    @Autowired
    RedisWorker redisWorker;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void test(){
        Shop shop = shopService.getById(1);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+"1",shop,2L, TimeUnit.SECONDS);
    }

    @Test
    public void timeStamp(){
        LocalDateTime localDateTime = LocalDateTime.of(2026,1,1,0,0,0);
        long startSecond = localDateTime.toEpochSecond(ZoneOffset.UTC);
        System.out.println(startSecond);

        for(int i = 0;i<1;i++){
            System.out.println(redisWorker.nextId("TEST"));
        }

    }

    @Test
    public void loadShopGeoData() {
        List<Shop> shops = shopService.list();
        Map<Long, List<Shop>> shopMap = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        shopMap.forEach((typeId, typeShops) -> {
            String key = SHOP_GEO_KEY + typeId;
            List<RedisGeoCommands.GeoLocation<String>> locations = typeShops.stream()
                    .map(shop -> new RedisGeoCommands.GeoLocation<>(
                            shop.getId().toString(),
                            new Point(shop.getX(), shop.getY())
                    ))
                    .collect(Collectors.toList());
            stringRedisTemplate.opsForGeo().add(key, locations);
        });
    }


}
