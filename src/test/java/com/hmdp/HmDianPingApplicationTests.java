package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    ShopServiceImpl shopService;
    @Autowired
    CacheClient cacheClient;
    @Autowired
    RedisWorker redisWorker;

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


}
