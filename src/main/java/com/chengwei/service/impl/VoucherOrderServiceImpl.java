package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.ClerkDTO;
import com.chengwei.dto.ClerkVerifyOrderDTO;
import com.chengwei.dto.Result;
import com.chengwei.dto.VoucherOrderVO;
import com.chengwei.entity.SeckillVoucher;
import com.chengwei.entity.Shop;
import com.chengwei.entity.ShopClerk;
import com.chengwei.entity.User;
import com.chengwei.entity.Voucher;
import com.chengwei.entity.VoucherOrder;
import com.chengwei.mapper.VoucherOrderMapper;
import com.chengwei.service.ISeckillVoucherService;
import com.chengwei.service.IShopClerkService;
import com.chengwei.service.IShopService;
import com.chengwei.service.IUserService;
import com.chengwei.service.IVoucherOrderService;
import com.chengwei.service.IVoucherService;
import com.chengwei.utils.annotation.OperationLogRecord;
import com.chengwei.utils.holder.ClerkHolder;
import com.chengwei.utils.holder.UserHolder;
import com.chengwei.utils.redis.RedisConstants;
import com.chengwei.utils.redis.RedisWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements IVoucherOrderService {

    private static final int STATUS_PENDING_PAY = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_USED = 3;
    private static final int STATUS_CANCELED = 4;

    private static final String GROUP_NAME = "group";
    private static final String CONSUMER_NAME = "consumer";
    private static final String STREAM_KEY = "stream.orders";
    private static final String VERIFY_CODE_PREFIX = "CW";

    private static final ExecutorService SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = initSeckillScript();

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisWorker redisWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IVoucherService voucherService;

    @Autowired
    private IShopService shopService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IShopClerkService shopClerkService;

    private IVoucherOrderService proxy;

    private static DefaultRedisScript<Long> initSeckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("seckill.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @PostConstruct
    public void init() {
        log.info("start voucher order worker");
        SINGLE_THREAD_EXECUTOR.execute(new HandleOrderTask());
    }

    @Override
    public Result secKill(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long orderId = redisWorker.nextId(RedisConstants.ORDER_ID);
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                orderId.toString()
        );
        if (result == null) {
            return Result.fail("下单失败，请稍后重试");
        }
        if (result.intValue() != 0) {
            return Result.fail(result.intValue() == 1 ? "库存不足" : "不能重复下单");
        }

        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

    @Override
    public Result queryMyOrders() {
        Long userId = UserHolder.getUser().getId();
        List<VoucherOrder> orders = lambdaQuery()
                .eq(VoucherOrder::getUserId, userId)
                .orderByDesc(VoucherOrder::getCreateTime)
                .list();
        fillMissingVerifyCodes(orders);
        return Result.ok(buildOrderVOs(orders));
    }

    @Override
    public Result queryClerkOrders(Integer status, String keyword) {
        ShopClerk clerk = getCurrentActiveClerk();
        if (clerk == null) {
            return Result.fail("请先登录可用的店员账号");
        }

        List<Voucher> vouchers = voucherService.lambdaQuery()
                .eq(Voucher::getShopId, clerk.getShopId())
                .list();
        if (CollUtil.isEmpty(vouchers)) {
            return Result.ok(Collections.emptyList());
        }

        Set<Long> voucherIds = new LinkedHashSet<>();
        for (Voucher voucher : vouchers) {
            voucherIds.add(voucher.getId());
        }

        List<VoucherOrder> orders = lambdaQuery()
                .in(VoucherOrder::getVoucherId, voucherIds)
                .orderByDesc(VoucherOrder::getCreateTime)
                .list();
        fillMissingVerifyCodes(orders);

        Map<Long, User> userMap = buildUserMap(orders);
        if (status != null) {
            orders.removeIf(order -> order.getStatus() == null || !status.equals(order.getStatus()));
        }
        if (StrUtil.isNotBlank(keyword)) {
            String trimmedKeyword = keyword.trim().toLowerCase();
            orders.removeIf(order -> {
                User user = userMap.get(order.getUserId());
                String phone = user == null ? "" : StrUtil.blankToDefault(user.getPhone(), "");
                String verifyCode = StrUtil.blankToDefault(order.getVerifyCode(), "");
                return !String.valueOf(order.getId()).contains(trimmedKeyword)
                        && !verifyCode.toLowerCase().contains(trimmedKeyword)
                        && !phone.contains(trimmedKeyword);
            });
        }

        return Result.ok(buildOrderVOs(orders, userMap));
    }

    @Override
    public Result payOrder(Long orderId) {
        Long userId = UserHolder.getUser().getId();
        String redisKey = RedisConstants.IDEMPOTENT_PAY_ORDER_KEY + userId + ":" + orderId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, String.valueOf(userId), Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        try {
            Result result = updateOrderStatus(
                    orderId,
                    STATUS_PENDING_PAY,
                    STATUS_PAID,
                    "订单不存在",
                    "只有待支付订单才能支付",
                    order -> order.setPayTime(LocalDateTime.now())
            );
            if (result != null && !Boolean.TRUE.equals(result.getSuccess())) {
                stringRedisTemplate.delete(redisKey);
            }
            return result;
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(redisKey);
            throw e;
        }
    }

    @Override
    public Result cancelOrder(Long orderId) {
        Long userId = UserHolder.getUser().getId();
        String redisKey = RedisConstants.IDEMPOTENT_CANCEL_ORDER_KEY + userId + ":" + orderId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, String.valueOf(userId), Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        try {
            VoucherOrder order = getById(orderId);
            if (order == null) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("订单不存在");
            }
            if (!userId.equals(order.getUserId())) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("不能操作别人的订单");
            }
            if (order.getStatus() == null || order.getStatus() != STATUS_PENDING_PAY) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("只有待支付订单才能取消");
            }

            boolean success = lambdaUpdate()
                    .eq(VoucherOrder::getId, orderId)
                    .eq(VoucherOrder::getUserId, userId)
                    .eq(VoucherOrder::getStatus, STATUS_PENDING_PAY)
                    .set(VoucherOrder::getStatus, STATUS_CANCELED)
                    .update();
            if (!success) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("订单状态已更新，请刷新后重试");
            }

            seckillVoucherService.lambdaUpdate()
                    .eq(SeckillVoucher::getVoucherId, order.getVoucherId())
                    .setSql("stock = stock + 1")
                    .update();
            stringRedisTemplate.opsForValue().increment(
                    RedisConstants.SECKILL_STOCK_VOUCHER_KEY + order.getVoucherId()
            );
            stringRedisTemplate.opsForSet().remove(
                    RedisConstants.SECKILL_ORDER_VOUCHER_KEY + order.getVoucherId(),
                    userId.toString()
            );
            return Result.ok();
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(redisKey);
            throw e;
        }
    }

    @Override
    public Result useOrder(Long orderId) {
        return Result.fail("请到店出示核销码，由店员完成核销");
    }

    @Override
    public Result clerkUseOrder(Long orderId) {
        return Result.fail("请使用核销码完成核销");
    }

    @Override
    @OperationLogRecord(module = "订单管理", action = "核销订单")
    public Result clerkVerifyOrder(ClerkVerifyOrderDTO verifyDTO) {
        Long orderId = verifyDTO == null ? null : verifyDTO.getOrderId();
        ClerkDTO clerkDTO = ClerkHolder.getClerk();
        Long clerkId = clerkDTO == null || clerkDTO.getId() == null ? 0L : clerkDTO.getId();
        String redisKey = RedisConstants.IDEMPOTENT_VERIFY_ORDER_KEY + clerkId + ":" + orderId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, String.valueOf(clerkId), Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        try {
            if (verifyDTO == null || verifyDTO.getOrderId() == null || StrUtil.isBlank(verifyDTO.getVerifyCode())) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("请填写完整的订单号和核销码");
            }
            ShopClerk clerk = getCurrentActiveClerk();
            if (clerk == null) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("请先登录可用的店员账号");
            }

            VoucherOrder order = getById(verifyDTO.getOrderId());
            if (order == null) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("订单不存在");
            }

            ensureVerifyCode(order);
            Voucher voucher = voucherService.getById(order.getVoucherId());
            if (voucher == null) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("订单对应的券不存在");
            }
            if (!clerk.getShopId().equals(voucher.getShopId())) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("当前店员只能核销所属门店的券");
            }
            if (order.getStatus() == null || order.getStatus() != STATUS_PAID) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("只有待核销订单才能核销");
            }
            if (!verifyDTO.getVerifyCode().trim().equalsIgnoreCase(order.getVerifyCode())) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("核销码不正确");
            }

            VoucherOrder updateOrder = new VoucherOrder();
            updateOrder.setId(order.getId());
            updateOrder.setStatus(STATUS_USED);
            updateOrder.setUseTime(LocalDateTime.now());
            updateOrder.setVerifyClerkId(clerk.getId());
            updateOrder.setVerifyShopId(clerk.getShopId());

            boolean success = lambdaUpdate()
                    .eq(VoucherOrder::getId, order.getId())
                    .eq(VoucherOrder::getStatus, STATUS_PAID)
                    .eq(VoucherOrder::getVoucherId, order.getVoucherId())
                    .update(updateOrder);
            if (!success) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("订单状态已变化，请刷新后重试");
            }
            return Result.ok();
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(redisKey);
            throw e;
        }
    }

    @Override
    @Transactional
    public void creatOrder(VoucherOrder voucherOrder) {
        if (voucherOrder == null || voucherOrder.getVoucherId() == null || voucherOrder.getUserId() == null) {
            return;
        }

        voucherOrder.setStatus(voucherOrder.getStatus() == null ? STATUS_PENDING_PAY : voucherOrder.getStatus());
        voucherOrder.setPayType(voucherOrder.getPayType() == null ? 1 : voucherOrder.getPayType());
        if (StrUtil.isBlank(voucherOrder.getVerifyCode()) && voucherOrder.getId() != null) {
            voucherOrder.setVerifyCode(generateVerifyCode(voucherOrder.getId()));
        }

        boolean success = seckillVoucherService.lambdaUpdate()
                .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                .gt(SeckillVoucher::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        if (!success) {
            log.warn("create order failed, stock not enough, voucherId={}", voucherOrder.getVoucherId());
            return;
        }

        save(voucherOrder);
        log.info("order saved, orderId={}, voucherId={}", voucherOrder.getId(), voucherOrder.getVoucherId());
    }

    private Result updateOrderStatus(Long orderId,
                                     int fromStatus,
                                     int toStatus,
                                     String notFoundMessage,
                                     String invalidStatusMessage,
                                     java.util.function.Consumer<VoucherOrder> customizer) {
        Long userId = UserHolder.getUser().getId();
        VoucherOrder order = getById(orderId);
        if (order == null) {
            return Result.fail(notFoundMessage);
        }
        ensureVerifyCode(order);
        if (!userId.equals(order.getUserId())) {
            return Result.fail("不能操作别人的订单");
        }
        if (order.getStatus() == null || order.getStatus() != fromStatus) {
            return Result.fail(invalidStatusMessage);
        }

        VoucherOrder updateOrder = new VoucherOrder();
        updateOrder.setId(orderId);
        updateOrder.setStatus(toStatus);
        customizer.accept(updateOrder);

        boolean success = lambdaUpdate()
                .eq(VoucherOrder::getId, orderId)
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getStatus, fromStatus)
                .update(updateOrder);
        if (!success) {
            return Result.fail("订单状态已更新，请刷新后重试");
        }
        return Result.ok();
    }

    private List<VoucherOrderVO> buildOrderVOs(List<VoucherOrder> orders) {
        return buildOrderVOs(orders, buildUserMap(orders));
    }

    private List<VoucherOrderVO> buildOrderVOs(List<VoucherOrder> orders, Map<Long, User> userMap) {
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyList();
        }

        Set<Long> voucherIds = new LinkedHashSet<>();
        Set<Long> clerkIds = new LinkedHashSet<>();
        for (VoucherOrder order : orders) {
            voucherIds.add(order.getVoucherId());
            if (order.getVerifyClerkId() != null) {
                clerkIds.add(order.getVerifyClerkId());
            }
        }

        Map<Long, Voucher> voucherMap = new HashMap<>();
        Set<Long> shopIds = new LinkedHashSet<>();
        for (Voucher voucher : voucherService.listByIds(voucherIds)) {
            voucherMap.put(voucher.getId(), voucher);
            shopIds.add(voucher.getShopId());
        }

        Map<Long, Shop> shopMap = new HashMap<>();
        for (Shop shop : shopService.listByIds(shopIds)) {
            shopMap.put(shop.getId(), shop);
        }

        Map<Long, ShopClerk> clerkMap = new HashMap<>();
        if (CollUtil.isNotEmpty(clerkIds)) {
            for (ShopClerk clerk : shopClerkService.listByIds(clerkIds)) {
                clerkMap.put(clerk.getId(), clerk);
            }
        }

        List<VoucherOrderVO> orderVOS = new ArrayList<>(orders.size());
        for (VoucherOrder order : orders) {
            VoucherOrderVO orderVO = new VoucherOrderVO();
            BeanUtil.copyProperties(order, orderVO);

            User user = userMap.get(order.getUserId());
            if (user != null) {
                orderVO.setUserNickName(user.getNickName());
                orderVO.setUserPhone(user.getPhone());
            }

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

            if (order.getVerifyClerkId() != null) {
                ShopClerk clerk = clerkMap.get(order.getVerifyClerkId());
                if (clerk != null) {
                    orderVO.setVerifyClerkName(clerk.getName());
                }
            }
            orderVOS.add(orderVO);
        }

        orderVOS.sort(Comparator.comparing(
                VoucherOrderVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return orderVOS;
    }

    private Map<Long, User> buildUserMap(List<VoucherOrder> orders) {
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (VoucherOrder order : orders) {
            userIds.add(order.getUserId());
        }

        Map<Long, User> userMap = new HashMap<>();
        for (User user : userService.listByIds(userIds)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private void fillMissingVerifyCodes(List<VoucherOrder> orders) {
        if (CollUtil.isEmpty(orders)) {
            return;
        }
        for (VoucherOrder order : orders) {
            ensureVerifyCode(order);
        }
    }

    private void ensureVerifyCode(VoucherOrder order) {
        if (order == null || order.getId() == null || StrUtil.isNotBlank(order.getVerifyCode())) {
            return;
        }
        String verifyCode = generateVerifyCode(order.getId());
        order.setVerifyCode(verifyCode);
        lambdaUpdate()
                .eq(VoucherOrder::getId, order.getId())
                .isNull(VoucherOrder::getVerifyCode)
                .set(VoucherOrder::getVerifyCode, verifyCode)
                .update();
    }

    private String generateVerifyCode(Long orderId) {
        return VERIFY_CODE_PREFIX + Long.toString(orderId, 36).toUpperCase();
    }

    private ShopClerk getCurrentActiveClerk() {
        ClerkDTO clerkDTO = ClerkHolder.getClerk();
        if (clerkDTO == null || clerkDTO.getId() == null) {
            return null;
        }
        ShopClerk clerk = shopClerkService.getById(clerkDTO.getId());
        if (clerk == null || clerk.getStatus() == null || clerk.getStatus() != 1) {
            return null;
        }
        return clerk;
    }

    private class HandleOrderTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(1),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                    );
                    if (records == null || records.isEmpty()) {
                        continue;
                    }

                    Map<Object, Object> map = records.get(0).getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), true);
                    proxy.creatOrder(voucherOrder);
                    stringRedisTemplate.opsForStream().acknowledge(
                            STREAM_KEY,
                            GROUP_NAME,
                            records.get(0).getId()
                    );
                } catch (Exception e) {
                    log.error("handle voucher order failed", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
                    );
                    if (records == null || records.isEmpty()) {
                        return;
                    }

                    Map<Object, Object> map = records.get(0).getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), true);
                    proxy.creatOrder(voucherOrder);
                    stringRedisTemplate.opsForStream().acknowledge(
                            STREAM_KEY,
                            GROUP_NAME,
                            records.get(0).getId()
                    );
                } catch (Exception e) {
                    log.error("handle pending voucher order failed", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
