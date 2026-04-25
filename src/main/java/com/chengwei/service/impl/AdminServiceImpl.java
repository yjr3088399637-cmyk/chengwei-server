package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.AdminClerkSaveDTO;
import com.chengwei.dto.AdminClerkVO;
import com.chengwei.dto.AdminDTO;
import com.chengwei.dto.AdminLoginFormDTO;
import com.chengwei.dto.AdminOverviewDTO;
import com.chengwei.dto.AdminShopSaveDTO;
import com.chengwei.dto.Result;
import com.chengwei.entity.Admin;
import com.chengwei.entity.Shop;
import com.chengwei.entity.ShopClerk;
import com.chengwei.entity.ShopType;
import com.chengwei.entity.Voucher;
import com.chengwei.mapper.AdminMapper;
import com.chengwei.mapper.ShopClerkMapper;
import com.chengwei.service.*;
import com.chengwei.utils.annotation.OperationLogRecord;
import com.chengwei.utils.holder.AdminHolder;
import com.chengwei.utils.redis.BloomFilter;
import com.chengwei.utils.redis.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.chengwei.utils.redis.RedisConstants.CACHE_SHOP_KEY;
import static com.chengwei.utils.redis.RedisConstants.SHOP_GEO_KEY;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IShopService shopService;
    private final IShopTypeService shopTypeService;
    private final IVoucherService voucherService;
    private final IShopClerkService shopClerkService;
    private final BloomFilter bloomFilter;
    @Override
    public Result login(AdminLoginFormDTO loginForm) {
        // 1.根据用户名查询管理员
        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, loginForm.getUsername().trim())
                .one();
        // 2.校验管理员是否存在
        if (admin == null) {
            return Result.fail("管理员账号不存在");
        }
        // 3.校验管理员状态是否可用
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            return Result.fail("管理员账号不可用");
        }
        // 4.比对密码是否一致
        if (!loginForm.getPassword().trim().equals(admin.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        // 5.将管理员信息存入Redis，实现有状态会话
        AdminDTO adminDTO = BeanUtil.copyProperties(admin, AdminDTO.class);
        // 5.1 生成全局唯一token作为Redis的key
        String token = "admin:token:" + UUID.randomUUID();
        // 5.2 将AdminDTO转为Map，字段值统一转为String以兼容Redis Hash存储
        Map<String, Object> map = BeanUtil.beanToMap(
                adminDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );
        // 5.3 以Hash结构存入Redis，并设置30分钟过期
        stringRedisTemplate.opsForHash().putAll(token, map);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);

        // 6.返回token和管理员信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", adminDTO);
        return Result.ok(result);
    }

    @Override
    public Result me() {
        // 1.从ThreadLocal中获取当前登录管理员信息
        AdminDTO admin = AdminHolder.getAdmin();

        // 2.从数据库查询最新信息（避免Redis缓存与数据库不一致）
        Admin latest = getById(admin.getId());
        // 3.校验管理员状态是否仍然可用（登录已校验，这里做兜底）
        if (latest == null || latest.getStatus() == null || latest.getStatus() != 1) {
            return Result.fail("管理员账号不可用");
        }
        // 4.返回最新的管理员信息
        return Result.ok(BeanUtil.copyProperties(latest, AdminDTO.class));
    }

    @Override
    public Result overview() {
        // 1.统计店铺总数
        long shopCount = shopService.count();
        // 2.统计店员总数
        long clerkCount = shopService.count();
        // 3.统计上架中的优惠券数量
        long activeVoucherCount = voucherService.lambdaQuery()
                .eq(Voucher::getStatus, 1)
                .count();
        // 4.统计已上线的店铺数量（score>0表示已有评分，即已上线）
        long onlineShopCount = shopService.lambdaQuery()
                .gt(Shop::getScore, 0)
                .count();
        // 5.组装概览数据返回
        return Result.ok(new AdminOverviewDTO(shopCount, clerkCount, activeVoucherCount, onlineShopCount));
    }

    @Override
    public Result queryShops(String keyword) {
        // 1.按关键字模糊查询店铺（同时匹配店名/商圈/地址），无关键字则查全部

        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> {
                //构造多个用括号括起来的查询条，在sql中and的优先级更高
                w.like(Shop::getName, keyword.trim())
                        .or()
                        .like(Shop::getArea, keyword.trim())
                        .or()
                        .like(Shop::getAddress, keyword.trim());
            });
        }
        wrapper.orderByDesc(Shop::getId);

        List<Shop> shops = shopService.list(wrapper);
        return Result.ok(shops);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLogRecord(module = "店铺管理", action = "新增店铺")
    public Result saveShop(AdminShopSaveDTO saveDTO) {
        // 1.检查分类是否存在
        if(shopTypeService.getById(saveDTO.getTypeId()) == null){
            return Result.fail("店铺分类不存在");
        }
        // 2.基于Redis的幂等校验：以"店名+地址"作为幂等键，防止短时间内重复提交
        String shopName = saveDTO.getName().trim();
        String shopAddress = saveDTO.getAddress().trim();
        //幂等校验
        String createShopKey = RedisConstants.IDEMPOTENT_CREATE_SHOP_KEY + shopName + ":" + shopAddress;
        // 2.1 利用setIfAbsent的原子性实现分布式幂等锁，5秒后自动过期
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(createShopKey, String.valueOf(AdminHolder.getAdmin().getId()), 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        try {
            // 3.构建店铺实体并初始化统计字段
            Shop shop = BeanUtil.copyProperties(saveDTO, Shop.class);
            shop.setSold(0);
            shop.setComments(0);
            shop.setScore(0);
            // 4.插入店铺信息到数据库
            //插入店铺信息到数据库
            shopService.save(shop);
            // 5.同步店铺GEO信息到Redis（用于附近商户LBS查询）
            //设置geo信息
            stringRedisTemplate.opsForGeo().add(
                    SHOP_GEO_KEY + shop.getTypeId(),
                    new Point(shop.getX(), shop.getY()),
                    String.valueOf(shop.getId())
            );
            // 5.构建店长实体并插入数据库（新建店铺必须同时创建首位店长）
            //DTO中获取clerk信息
            ShopClerk clerk = buildClerkEntity(shop.getId(), saveDTO);
            //插入店长信息到数据库
            shopClerkService.save(clerk);
            // 6.将新店铺id加入布隆过滤器（用于后续查询时快速判断店铺是否存在）
            //将id添加至过滤器
            bloomFilter.add(RedisConstants.SHOP_BLOOM_KEY, shop.getId());
            return Result.ok(shop);
        } catch (Exception e) {
            // 7.异常时释放幂等锁，允许重新提交
            stringRedisTemplate.delete(createShopKey);
            throw e;
        }
    }


    @Override
    public Result queryClerks(String keyword) {
        // 1.查询所有店长（role=1），支持按账号或名称模糊搜索
        LambdaQueryWrapper<ShopClerk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopClerk::getRole, 1);
        //如果有值则添加查询条件
        if(StrUtil.isNotBlank(keyword)){
            wrapper.and(w->w
                            .like(ShopClerk::getUsername, keyword.trim())
                            .or()
                            .like(ShopClerk::getName, keyword.trim()));
        }
        wrapper.orderByDesc(ShopClerk::getId);
        List<ShopClerk> clerks = shopClerkService.list(wrapper);

        if (clerks.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        // 2.将店长id转集合，批量查询关联的店铺名称，以map集合返回映射关系
        Set<Long> ClerkIds = clerks.stream().map(ShopClerk::getShopId).collect(Collectors.toSet());
        Map<Long, String> shopNameMap = shopService.lambdaQuery()
                .in(Shop::getId, ClerkIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName));

        // 3.组装VO：将店铺名称设置到店长VO中
        List<AdminClerkVO> result = clerks.stream().map(clerk -> {
            AdminClerkVO vo = BeanUtil.copyProperties(clerk, AdminClerkVO.class);
            vo.setShopName(shopNameMap.get(clerk.getShopId()));
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    @Override
    public Result queryShopTypes() {
        // 1.查询所有店铺分类，按排序字段升序排列
        List<ShopType> types = shopTypeService.lambdaQuery()
                .orderByAsc(ShopType::getSort)
                .list();
        return Result.ok(types);
    }


    private ShopClerk buildClerkEntity(Long shopId, AdminShopSaveDTO dto) {
        // 从店铺保存DTO中提取店长信息，构建ShopClerk实体
        ShopClerk clerk = new ShopClerk();
        clerk.setShopId(shopId);
        clerk.setUsername(dto.getClerkUsername().trim());
        clerk.setPassword(dto.getClerkPassword().trim());
        clerk.setName(dto.getClerkName().trim());
        // 固定设为店长角色和启用状态
        clerk.setRole(1);
        clerk.setStatus(1);
        return clerk;
    }
}
