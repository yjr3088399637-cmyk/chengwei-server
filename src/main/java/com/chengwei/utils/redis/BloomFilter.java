package com.chengwei.utils.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class BloomFilter {

    private final RedissonClient redissonClient;

    public  <T> void initBloomFilter(String key,IService<T> service, Function<T,Long> queryId) {
        //创建布隆过滤器
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(key);
        //初始化布隆过滤器
        bloomFilter.tryInit(10000L,0.01);
        //分页查询数据并添加到布隆过滤器
        long pageSize = 1000;
        long pageNum = 1;
        while(true){
            //分页查询数据
            List<T> list = service.lambdaQuery().last("limit " + (pageNum - 1) * pageSize+ ","+ pageSize).list();
            //查不到值退出循环
            if(list == null||list.isEmpty()){
                break;
            }

            List<Long> ids = list.stream().map(queryId).collect(Collectors.toList());
            for(Long id:ids){
                //添加数据到布隆过滤器
                bloomFilter.add(id);
            }
            pageNum++;
        }
        log.info("布隆过滤器初始化完成~~");
    }

    public Boolean filter(String key,Long id){
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(key);
        if(!bloomFilter.contains(id)){
            log.info("非法id,布隆过滤器已拦截");
            return false;
        }
        return true;
    }

    public void add(String key, Long id) {
        if (id == null) {
            return;
        }
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(key);
        bloomFilter.add(id);
    }

}
