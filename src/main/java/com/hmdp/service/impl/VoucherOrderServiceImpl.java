package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisWorker redisWorker;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = initUnlockScript();
    private static DefaultRedisScript<Long> initUnlockScript() {
        DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
        return SECKILL_SCRIPT;
    }

//    @Override
//    public Result secKill(Long voucherId) {
//        //使用lua脚本判断库存是否充足，是否已经下单
//        Long result = stringRedisTemplate.execute
//                (SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), UserHolder.getUser().getId().toString());
//        if (result.intValue() != 0) {
//            return Result.fail(result.intValue() == 1 ? "库存不足" : "不能重复下单");
//        }
//        long orderId = redisWorker.nextId(RedisConstants.ORDER_ID);
//
//        return Result.ok(orderId);
//    }

    @Override
    public Result secKill(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if(voucher == null){
            return Result.fail("优惠券为空");
        }
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("活动未开始");
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("活动已结束");
        }
        if(voucher.getStock()<1){
            return Result.fail("没有库存");
        }

        long userId = UserHolder.getUser().getId();
//        //先提交事务再释放锁，保证线程安全，intern()保证多个线程通过String内容获取到同一个String对象
//        synchronized (voucherId.toString().intern()){
//            //获取代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            //使用代理对象调用事务方法
//            return proxy.creatOrder(voucherId, userId);
//        }
        //获取自定义锁
        //SimpleRedisLock lock = new SimpleRedisLock("lock:order:"+userId,stringRedisTemplate);
        //获取Redisson锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = false;
        try {
            isLock = lock.tryLock(0,30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(!isLock){
            log.info("用户id：{}已下单",userId);
            return Result.fail("一个用户仅允许下一单");
        }
        try {
            //获取代理对象
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            //使用代理对象调用事务方法
            return proxy.creatOrder(voucherId, userId);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }


    @Override
    @Transactional
    public Result creatOrder(Long voucherId, Long userId) {
        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if(count > 0){
            return Result.fail("该用户已下单");
        }
        //
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").gt("stock", 0)
                .eq("voucher_id",voucherId).update();
        if(!success){
            return Result.fail("没有库存");
        }
        long orderId = redisWorker.nextId(RedisConstants.ORDER_ID);
        VoucherOrder voucherOrder = VoucherOrder.builder()
                .id(orderId)
                .voucherId(voucherId)
                .userId(UserHolder.getUser().getId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
