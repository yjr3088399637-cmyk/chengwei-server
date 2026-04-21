package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
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
import com.chengwei.utils.holder.AdminHolder;
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

    @Override
    public Result login(AdminLoginFormDTO loginForm) {

        // 管理端登录：校验账号密码后生成随机 token，并把轻量管理员信息写入 Redis。
        if (loginForm == null || StrUtil.isBlank(loginForm.getUsername()) || StrUtil.isBlank(loginForm.getPassword())) {
            return Result.fail("请输入管理员账号和密码");
        }
        // 管理员账号单独存放在 tb_admin，不和用户端、店员端复用同一张表。
        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, loginForm.getUsername().trim())
                .one();
        if (admin == null) {
            return Result.fail("管理员账号不存在");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            return Result.fail("管理员账号不可用");
        }
        //校验密码
        if (!loginForm.getPassword().trim().equals(admin.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        AdminDTO adminDTO = BeanUtil.copyProperties(admin, AdminDTO.class);
        // 后续请求靠这个 token 去 Redis 恢复当前管理员身份，因此这里采用“随机 token + Redis Hash”方案。
        String token = "admin:token:" + UUID.randomUUID();
        Map<String, Object> map = BeanUtil.beanToMap(
                adminDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );
        // Redis 中只保存 AdminDTO 这类轻量字段，避免把整张管理员对象塞进登录态。
        stringRedisTemplate.opsForHash().putAll(token, map);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", adminDTO);
        return Result.ok(result);
    }

    @Override
    public Result me() {
        // /admin/me 读取的是拦截器提前放进 AdminHolder 的当前管理员，而不是前端自己传管理员,这步仅仅是确认是否登录
        AdminDTO admin = AdminHolder.getAdmin();
        if (admin == null) {
            return Result.fail("请先登录管理员账号");
        }
        // 再查一遍数据库是为了防止管理员被停用后，旧 token 还能继续访问后台。
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
    public Result saveShop(AdminShopSaveDTO saveDTO) {
        String validation = validateShopDTO(saveDTO, true);
        if (validation != null) {
            return Result.fail(validation);
        }

        //Shop shop = buildShopEntity(null, saveDTO);
        Shop shop = BeanUtil.copyProperties(saveDTO, Shop.class);
        shop.setSold(0);
        shop.setComments(0);
        shop.setScore(0);
        shopService.save(shop);
        syncShopGeo(null, shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        ShopClerk clerk = buildClerkEntity(shop.getId(), saveDTO);
        shopClerkMapper.insert(clerk);
        return Result.ok(shop);
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
        List<ShopClerk> clerks = new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(shopClerkMapper)
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
    public Result createClerk(AdminClerkSaveDTO saveDTO) {
        if (saveDTO == null) {
            return Result.fail("未提交店员信息");
        }
        if (saveDTO.getShopId() == null) {
            return Result.fail("请选择要绑定的店铺");
        }
        if (StrUtil.isBlank(saveDTO.getUsername()) || StrUtil.isBlank(saveDTO.getPassword()) || StrUtil.isBlank(saveDTO.getName())) {
            return Result.fail("请完整填写店员账号、密码和名称");
        }
        Shop shop = shopService.getById(saveDTO.getShopId());
        if (shop == null) {
            return Result.fail("目标店铺不存在");
        }
        int exists = new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(shopClerkMapper)
                .eq(ShopClerk::getUsername, saveDTO.getUsername().trim())
                .count();
        if (exists > 0) {
            return Result.fail("店长账号已存在，请更换后重试");
        }
        int managerExists = new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(shopClerkMapper)
                .eq(ShopClerk::getShopId, saveDTO.getShopId())
                .eq(ShopClerk::getRole, 1)
                .count();
        if (managerExists > 0) {
            return Result.fail("该店铺已经有店长，请勿重复创建");
        }

        ShopClerk clerk = new ShopClerk();
        clerk.setShopId(shop.getId());
        clerk.setUsername(saveDTO.getUsername().trim());
        clerk.setPassword(saveDTO.getPassword().trim());
        clerk.setName(saveDTO.getName().trim());
        clerk.setRole(1);
        clerk.setStatus(1);
        shopClerkMapper.insert(clerk);
        return Result.ok(clerk);
    }

    @Override
    public Result queryShopTypes() {
        List<ShopType> types = shopTypeService.lambdaQuery()
                .orderByAsc(ShopType::getSort)
                .list();
        return Result.ok(types);
    }

    private String validateShopDTO(AdminShopSaveDTO dto, boolean requireClerk) {
        if (dto == null) {
            return "未提交店铺信息";
        }
        if (StrUtil.isBlank(dto.getName())) {
            return "店铺名称不能为空";
        }
        if (dto.getTypeId() == null) {
            return "请选择店铺分类";
        }
        if (shopTypeService.getById(dto.getTypeId()) == null) {
            return "店铺分类不存在";
        }
        if (StrUtil.isBlank(dto.getAddress())) {
            return "店铺地址不能为空";
        }
        if (StrUtil.isBlank(dto.getOpenHours())) {
            return "营业时间不能为空";
        }
        if (dto.getAvgPrice() == null || dto.getAvgPrice() < 0) {
            return "请输入正确的人均价格";
        }
        if (dto.getX() == null || dto.getY() == null) {
            return "请填写店铺经纬度";
        }
        if (requireClerk) {
            if (StrUtil.isBlank(dto.getClerkUsername()) || StrUtil.isBlank(dto.getClerkPassword()) || StrUtil.isBlank(dto.getClerkName())) {
                return "新建店铺时必须同时创建首个店员";
            }
            int exists = new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(shopClerkMapper)
                    .eq(ShopClerk::getUsername, dto.getClerkUsername().trim())
                    .count();
            if (exists > 0) {
                return "首个店员账号已存在，请更换";
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
}
