package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.chengwei.dto.Result;
import com.chengwei.dto.VoucherOrderVO;
import com.chengwei.entity.SeckillVoucher;
import com.chengwei.entity.Shop;
import com.chengwei.entity.Voucher;
import com.chengwei.entity.VoucherOrder;
import com.chengwei.mapper.VoucherOrderMapper;
import com.chengwei.service.ISeckillVoucherService;
import com.chengwei.service.IShopService;
import com.chengwei.service.IVoucherService;
import com.chengwei.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.utils.RedisConstants;
import com.chengwei.utils.RedisWorker;
import com.chengwei.utils.SimpleRedisLock;
import com.chengwei.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    //在依赖注入时执行
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisWorker redisWorker;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private IShopService shopService;
    @Autowired
    RedissonClient redissonClient;
    //将代理声明进成员变量
    IVoucherOrderService proxy;
    //在类被加载时执行
    private static final String GROUP_NAME = "group";
    private static final String CONSUMER_NAME = "consumer";
    private static final String STREAM_KEY = "stream.orders";

    /**
     * 单线程执行程序
     */
    static final ExecutorService SINGLE_THREAD_EXECUTOR;
    static {
        SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
    }


    //在Bean初始化时执行
    @PostConstruct
    public void init() {
        log.info("=== 初始化异步订单线程 ===");
        SINGLE_THREAD_EXECUTOR.execute(new HandleOrder());
    }

    class HandleOrder implements Runnable {
        @Override
        public void run() {
            log.info("=== HandleOrder 线程启动成功 ===");
            while (true) {
                try {
                    //log.info("准备读取 stream 新消息...");
                    List<MapRecord<String, Object, Object>> records =
                            stringRedisTemplate.opsForStream().read(
                                    Consumer.from(GROUP_NAME, CONSUMER_NAME), // 消费者组
                                    StreamReadOptions.empty()
                                            .block(Duration.ofMillis(2000))
                                            .count(1),                    // 一次读1条
                                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()) // 从上次读完的位置继续
                            );
                    //log.info("读取结果 records = {}", records);
                    if (records == null || records.isEmpty()) {
                        //log.info("records is null or empty,continue ...");
                        continue;
                    }
                    //解析数据
                    Map<Object,Object> map = records.get(0).getValue();
                    VoucherOrder voucherOrder = new VoucherOrder();
                    BeanUtil.fillBeanWithMap(map,voucherOrder,true);
                    //TODO 从成员变量中获取代理对象是否正正确?
                    log.info("调用 creatOrder 前，proxy = {}", proxy);
                    proxy.creatOrder(voucherOrder);
                    if(proxy == null){
                        log.info("proxy is null");
                        continue;
                    }
                    //发送确认ACK
                    stringRedisTemplate.opsForStream().acknowledge(
                            STREAM_KEY,
                            GROUP_NAME,
                            records.get(0).getId()
                    );
                } catch (Exception e) {
                    log.error(e.getMessage());
                    while(true){
                        try {
                            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                                    StreamReadOptions.empty()
                                            .count(1),
                                    StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
                            );
                            //Pending队列已经为空
                            if (records == null || records.isEmpty()) {
                                break;
                            }
                            //解析数据
                            Map<Object,Object> map = records.get(0).getValue();
                            VoucherOrder voucherOrder = new VoucherOrder();
                            BeanUtil.fillBeanWithMap(map,voucherOrder,true);
                            //TODO 从成员变量中获取代理对象是否正正确?
                            proxy.creatOrder(voucherOrder);
                            //发送确认ACK
                            stringRedisTemplate.opsForStream().acknowledge(
                                    STREAM_KEY,
                                    GROUP_NAME,
                                    records.get(0).getId()
                            );
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException exc) {
                                throw new RuntimeException(exc);
                            }
                        }
                    }
                }

            }
        }
    }

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = initUnlockScript();
    private static DefaultRedisScript<Long> initUnlockScript() {
        DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
        return SECKILL_SCRIPT;
    }

    @Override
    public Result secKill(Long voucherId) {
        //先生成订单号
        Long orderId = redisWorker.nextId(RedisConstants.ORDER_ID);
        //使用lua脚本判断库存是否充足，是否已经下单
        Long result = stringRedisTemplate.execute
                (SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), UserHolder.getUser().getId().toString(),orderId.toString());
        if (result.intValue() != 0) {
            return Result.fail(result.intValue() == 1 ? "库存不足" : "不能重复下单");
        }
        //给成员变量的代理赋值,如果异步线程提前执行，会产生空指针异常
        proxy = (IVoucherOrderService)AopContext.currentProxy();
        return Result.ok(orderId);
    }

    @Override
    public Result queryMyOrders() {
        Long userId = UserHolder.getUser().getId();
        List<VoucherOrder> orders = query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .list();
        if (CollUtil.isEmpty(orders)) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> voucherIds = new ArrayList<>();
        for (VoucherOrder order : orders) {
            voucherIds.add(order.getVoucherId());
        }
        List<Voucher> vouchers = voucherService.listByIds(voucherIds);
        Map<Long, Voucher> voucherMap = new HashMap<>();
        List<Long> shopIds = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            voucherMap.put(voucher.getId(), voucher);
            shopIds.add(voucher.getShopId());
        }
        List<Shop> shops = shopService.listByIds(shopIds);
        Map<Long, Shop> shopMap = new HashMap<>();
        for (Shop shop : shops) {
            shopMap.put(shop.getId(), shop);
        }

        List<VoucherOrderVO> orderVOS = new ArrayList<>(orders.size());
        for (VoucherOrder order : orders) {
            VoucherOrderVO orderVO = new VoucherOrderVO();
            orderVO.setId(order.getId());
            orderVO.setVoucherId(order.getVoucherId());
            orderVO.setStatus(order.getStatus());
            orderVO.setCreateTime(order.getCreateTime());

            Voucher voucher = voucherMap.get(order.getVoucherId());
            if (voucher != null) {
                orderVO.setShopId(voucher.getShopId());
                orderVO.setVoucherTitle(voucher.getTitle());
                orderVO.setVoucherSubTitle(voucher.getSubTitle());
                orderVO.setPayValue(voucher.getPayValue());
                orderVO.setActualValue(voucher.getActualValue());

                Shop shop = shopMap.get(voucher.getShopId());
                if (shop != null) {
                    orderVO.setShopName(shop.getName());
                }
            }
            orderVOS.add(orderVO);
        }
        orderVOS.sort(Comparator.comparing(VoucherOrderVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return Result.ok(orderVOS);
    }



//    @Override
//    public Result secKill(Long voucherId) {
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        if(voucher == null){
//            return Result.fail("优惠券为空");
//        }
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("活动未开始");
//        }
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("活动已结束");
//        }
//        if(voucher.getStock()<1){
//            return Result.fail("没有库存");
//        }
//
//        long userId = UserHolder.getUser().getId();
////        //先提交事务再释放锁，保证线程安全，intern()保证多个线程通过String内容获取到同一个String对象
////        synchronized (voucherId.toString().intern()){
////            //获取代理对象
////            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
////            //使用代理对象调用事务方法
////            return proxy.creatOrder(voucherId, userId);
////        }
//        //获取自定义锁
//        //SimpleRedisLock lock = new SimpleRedisLock("lock:order:"+userId,stringRedisTemplate);
//        //获取Redisson锁
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = false;
//        try {
//            isLock = lock.tryLock(0,30, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        if(!isLock){
//            log.info("用户id：{}已下单",userId);
//            return Result.fail("一个用户仅允许下一单");
//        }
//        try {
//            //获取代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            //使用代理对象调用事务方法
//            return proxy.creatOrder(voucherId, userId);
//        } catch (IllegalStateException e) {
//            throw new RuntimeException(e);
//        } finally {
//            lock.unlock();
//        }
//    }

    @Override
    @Transactional
    public void creatOrder(VoucherOrder voucherOrder) {
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").gt("stock", 0)
                .eq("voucher_id",voucherOrder.getVoucherId()).update();
        if(!success){
            log.info("没有库存");
        }
        save(voucherOrder);
        log.info("订单{},成功保存",voucherOrder.getVoucherId());
    }
}
