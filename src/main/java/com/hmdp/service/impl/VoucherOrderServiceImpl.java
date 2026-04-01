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
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("lock:order:"+userId,stringRedisTemplate);
        boolean isLock = simpleRedisLock.tryLock(100);
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
            simpleRedisLock.unlock();
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
