package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
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
import com.chengwei.service.IAdminService;
import com.chengwei.service.IShopService;
import com.chengwei.service.IShopTypeService;
import com.chengwei.service.IVoucherService;
import com.chengwei.utils.annotation.OperationLogRecord;
import com.chengwei.utils.holder.AdminHolder;
import com.chengwei.utils.redis.BloomFilter;
import com.chengwei.utils.redis.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final ShopClerkMapper shopClerkMapper;
    private final BloomFilter bloomFilter;
    @Override
    public Result login(AdminLoginFormDTO loginForm) {
        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, loginForm.getUsername().trim())
                .one();
        if (admin == null) {
            return Result.fail("管理员账号不存在");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            return Result.fail("管理员账号不可用");
        }
        if (!loginForm.getPassword().trim().equals(admin.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        AdminDTO adminDTO = BeanUtil.copyProperties(admin, AdminDTO.class);
        String token = "admin:token:" + UUID.randomUUID();
        Map<String, Object> map = BeanUtil.beanToMap(
                adminDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );
        stringRedisTemplate.opsForHash().putAll(token, map);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", adminDTO);
        return Result.ok(result);
    }

    @Override
    public Result me() {
        AdminDTO admin = AdminHolder.getAdmin();
        if (admin == null) {
            return Result.fail("请先登录管理员账号");
        }
        Admin latest = getById(admin.getId());
        if (latest == null || latest.getStatus() == null || latest.getStatus() != 1) {
            return Result.fail("管理员账号不可用");
        }
        return Result.ok(BeanUtil.copyProperties(latest, AdminDTO.class));
    }

    @Override
    public Result overview() {
        long shopCount = shopService.count();
        long clerkCount = shopClerkMapper.selectCount(null);
        long activeVoucherCount = voucherService.lambdaQuery()
                .eq(Voucher::getStatus, 1)
                .count();
        long onlineShopCount = shopService.lambdaQuery()
                .gt(Shop::getScore, 0)
                .count();
        return Result.ok(new AdminOverviewDTO(shopCount, clerkCount, activeVoucherCount, onlineShopCount));
    }

    @Override
    public Result queryShops(String keyword) {
        List<Shop> shops = shopService.lambdaQuery()
                .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                        .like(Shop::getName, keyword.trim())
                        .or()
                        .like(Shop::getArea, keyword.trim())
                        .or()
                        .like(Shop::getAddress, keyword.trim()))
                .orderByDesc(Shop::getId)
                .list();
        return Result.ok(shops);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLogRecord(module = "店铺管理", action = "新增店铺")
    public Result saveShop(AdminShopSaveDTO saveDTO) {
        String validation = validateShopDTO(saveDTO, true);
        if (validation != null) {
            return Result.fail(validation);
        }

        String shopName = saveDTO.getName().trim();
        String shopAddress = saveDTO.getAddress().trim();
        //幂等校验
        String createShopKey = RedisConstants.IDEMPOTENT_CREATE_SHOP_KEY + shopName + ":" + shopAddress;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(createShopKey, String.valueOf(currentAdminId()), 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        try {
            Shop shop = BeanUtil.copyProperties(saveDTO, Shop.class);
            shop.setName(shopName);
            shop.setAddress(shopAddress);
            shop.setOpenHours(saveDTO.getOpenHours().trim());
            shop.setImages(normalizeImages(saveDTO.getImages()));
            shop.setArea(StrUtil.blankToDefault(StrUtil.trim(saveDTO.getArea()), ""));
            shop.setSold(0);
            shop.setComments(0);
            shop.setScore(0);
            //插入店铺信息到数据库
            shopService.save(shop);
            //设置geo信息
            syncShopGeo(null, shop);
            stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
            //DTO中获取clerk信息
            ShopClerk clerk = buildClerkEntity(shop.getId(), saveDTO);
            //插入店长信息到数据库
            shopClerkMapper.insert(clerk);
            //将id添加至过滤器
            bloomFilter.add(RedisConstants.SHOP_BLOOM_KEY, shop.getId());
            return Result.ok(shop);
        } catch (Exception e) {
            stringRedisTemplate.delete(createShopKey);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateShop(Long id, AdminShopSaveDTO updateDTO) {
        if (id == null) {
            return Result.fail("店铺编号不能为空");
        }
        String validation = validateShopDTO(updateDTO, false);
        if (validation != null) {
            return Result.fail(validation);
        }

        Shop oldShop = shopService.getById(id);
        if (oldShop == null) {
            return Result.fail("店铺不存在");
        }

        Shop updateShop = buildShopEntity(id, updateDTO);
        updateShop.setSold(oldShop.getSold());
        updateShop.setComments(oldShop.getComments());
        updateShop.setScore(oldShop.getScore());
        shopService.updateById(updateShop);
        syncShopGeo(oldShop, updateShop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok(updateShop);
    }

    @Override
    public Result queryClerks(String keyword) {
        List<ShopClerk> clerks = new LambdaQueryChainWrapper<>(shopClerkMapper)
                .eq(ShopClerk::getRole, 1)
                .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                        .like(ShopClerk::getUsername, keyword.trim())
                        .or()
                        .like(ShopClerk::getName, keyword.trim()))
                .orderByDesc(ShopClerk::getId)
                .list();
        if (clerks.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        Map<Long, String> shopNameMap = shopService.lambdaQuery()
                .in(Shop::getId, clerks.stream().map(ShopClerk::getShopId).collect(Collectors.toSet()))
                .list()
                .stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName));

        List<AdminClerkVO> result = clerks.stream().map(clerk -> {
            AdminClerkVO vo = BeanUtil.copyProperties(clerk, AdminClerkVO.class);
            vo.setShopName(shopNameMap.getOrDefault(clerk.getShopId(), "未绑定店铺"));
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLogRecord(module = "店长管理", action = "创建店长")
    public Result createClerk(AdminClerkSaveDTO saveDTO) {
        String username = saveDTO.getUsername().trim();
        String createManagerKey = RedisConstants.IDEMPOTENT_CREATE_MANAGER_KEY + saveDTO.getShopId() + ":" + username;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(createManagerKey, String.valueOf(currentAdminId()), 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        Shop shop = shopService.getById(saveDTO.getShopId());
        if (shop == null) {
            stringRedisTemplate.delete(createManagerKey);
            return Result.fail("目标店铺不存在");
        }

        int exists = new LambdaQueryChainWrapper<>(shopClerkMapper)
                .eq(ShopClerk::getUsername, username)
                .count();
        if (exists > 0) {
            stringRedisTemplate.delete(createManagerKey);
            return Result.fail("店长账号已存在，请更换后重试");
        }

        int managerExists = new LambdaQueryChainWrapper<>(shopClerkMapper)
                .eq(ShopClerk::getShopId, saveDTO.getShopId())
                .eq(ShopClerk::getRole, 1)
                .count();
        if (managerExists > 0) {
            stringRedisTemplate.delete(createManagerKey);
            return Result.fail("该店铺已经有店长，请勿重复创建");
        }

        try {
            ShopClerk clerk = new ShopClerk();
            clerk.setShopId(shop.getId());
            clerk.setUsername(username);
            clerk.setPassword(saveDTO.getPassword().trim());
            clerk.setName(saveDTO.getName().trim());
            clerk.setRole(1);
            clerk.setStatus(1);
            shopClerkMapper.insert(clerk);
            return Result.ok(clerk);
        } catch (Exception e) {
            stringRedisTemplate.delete(createManagerKey);
            throw e;
        }
    }

    @Override
    public Result queryShopTypes() {
        List<ShopType> types = shopTypeService.lambdaQuery()
                .orderByAsc(ShopType::getSort)
                .list();
        return Result.ok(types);
    }

    private String validateShopDTO(AdminShopSaveDTO dto, boolean requireClerk) {
        if (shopTypeService.getById(dto.getTypeId()) == null) {
            return "店铺分类不存在";
        }
        if (requireClerk) {
            if (StrUtil.isBlank(dto.getClerkUsername()) || StrUtil.isBlank(dto.getClerkPassword()) || StrUtil.isBlank(dto.getClerkName())) {
                return "新建店铺时必须同时创建首个店长";
            }
            int exists = new LambdaQueryChainWrapper<>(shopClerkMapper)
                    .eq(ShopClerk::getUsername, dto.getClerkUsername().trim())
                    .count();
            if (exists > 0) {
                return "首个店长账号已存在，请更换";
            }
        }
        return null;
    }

    private Shop buildShopEntity(Long id, AdminShopSaveDTO dto) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName(dto.getName().trim());
        shop.setTypeId(dto.getTypeId());
        shop.setImages(normalizeImages(dto.getImages()));
        shop.setArea(StrUtil.blankToDefault(StrUtil.trim(dto.getArea()), ""));
        shop.setAddress(dto.getAddress().trim());
        shop.setX(dto.getX());
        shop.setY(dto.getY());
        shop.setAvgPrice(dto.getAvgPrice());
        shop.setOpenHours(dto.getOpenHours().trim());
        return shop;
    }

    private ShopClerk buildClerkEntity(Long shopId, AdminShopSaveDTO dto) {
        ShopClerk clerk = new ShopClerk();
        clerk.setShopId(shopId);
        clerk.setUsername(dto.getClerkUsername().trim());
        clerk.setPassword(dto.getClerkPassword().trim());
        clerk.setName(dto.getClerkName().trim());
        clerk.setRole(1);
        clerk.setStatus(1);
        return clerk;
    }

    private String normalizeImages(String images) {
        if (StrUtil.isBlank(images)) {
            return "";
        }
        String normalized = images
                .replace("\r", "\n")
                .replace("，", ",")
                .replace(";", ",")
                .replace("\n", ",");
        return StrUtil.join(",", StrUtil.splitTrim(normalized, ','));
    }

    private void syncShopGeo(Shop oldShop, Shop newShop) {
        if (oldShop != null && oldShop.getTypeId() != null) {
            stringRedisTemplate.opsForZSet().remove(SHOP_GEO_KEY + oldShop.getTypeId(), String.valueOf(oldShop.getId()));
        }
        if (newShop.getTypeId() != null && newShop.getX() != null && newShop.getY() != null && newShop.getId() != null) {
            stringRedisTemplate.opsForGeo().add(
                    SHOP_GEO_KEY + newShop.getTypeId(),
                    new Point(newShop.getX(), newShop.getY()),
                    String.valueOf(newShop.getId())
            );
        }
    }

    private Long currentAdminId() {
        AdminDTO admin = AdminHolder.getAdmin();
        return admin == null || admin.getId() == null ? 0L : admin.getId();
    }
}
