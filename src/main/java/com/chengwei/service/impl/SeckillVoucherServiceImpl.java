package com.chengwei.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.entity.SeckillVoucher;
import com.chengwei.mapper.SeckillVoucherMapper;
import com.chengwei.service.ISeckillVoucherService;
import com.chengwei.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void initSeckillStockCache() {
        List<SeckillVoucher> vouchers = list();
        if (vouchers == null || vouchers.isEmpty()) {
            return;
        }
        for (SeckillVoucher voucher : vouchers) {
            if (voucher.getVoucherId() == null || voucher.getStock() == null) {
                continue;
            }
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.SECKILL_STOCK_VOUCHER_KEY + voucher.getVoucherId(),
                    voucher.getStock().toString()
            );
        }
    }
}
