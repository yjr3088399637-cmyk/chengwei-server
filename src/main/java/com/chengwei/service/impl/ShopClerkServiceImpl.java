package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.ClerkDTO;
import com.chengwei.dto.ClerkLoginFormDTO;
import com.chengwei.dto.ClerkShopUpdateDTO;
import com.chengwei.dto.ClerkStaffSaveDTO;
import com.chengwei.dto.ClerkStaffVO;
import com.chengwei.dto.Result;
import com.chengwei.entity.Shop;
import com.chengwei.entity.ShopClerk;
import com.chengwei.mapper.ShopClerkMapper;
import com.chengwei.service.IShopClerkService;
import com.chengwei.service.IShopService;
import com.chengwei.utils.annotation.OperationLogRecord;
import com.chengwei.utils.holder.ClerkHolder;
import com.chengwei.utils.redis.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopClerkServiceImpl extends ServiceImpl<ShopClerkMapper, ShopClerk> implements IShopClerkService {
    private static final int ROLE_MANAGER = 1;
    private static final int ROLE_STAFF = 2;

    private final StringRedisTemplate stringRedisTemplate;
    private final IShopService shopService;

    @Override
    public Result login(ClerkLoginFormDTO loginForm) {
        ShopClerk clerk = lambdaQuery()
                .eq(ShopClerk::getUsername, loginForm.getUsername().trim())
                .one();
        if (clerk == null) {
            return Result.fail("店员账号不存在");
        }
        if (!Integer.valueOf(1).equals(clerk.getStatus())) {
            return Result.fail("店员账号不可用");
        }
        if (!loginForm.getPassword().trim().equals(clerk.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        ClerkDTO clerkDTO = BeanUtil.copyProperties(clerk, ClerkDTO.class);
        String token = "clerk:token:" + UUID.randomUUID();
        Map<String, Object> clerkMap = BeanUtil.beanToMap(
                clerkDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );
        stringRedisTemplate.opsForHash().putAll(token, clerkMap);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("clerk", clerkDTO);
        return Result.ok(result);
    }

    @Override
    public Result me() {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null) {
            return Result.fail("请先登录店员账号");
        }

        ShopClerk latest = getById(clerk.getId());
        if (latest == null || !Integer.valueOf(1).equals(latest.getStatus())) {
            return Result.fail("店员账号不可用");
        }
        return Result.ok(BeanUtil.copyProperties(latest, ClerkDTO.class));
    }

    @Override
    public Result queryCurrentShop() {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null) {
            return Result.fail("请先登录店员账号");
        }
        if (clerk.getShopId() == null) {
            return Result.fail("当前店员未绑定店铺");
        }

        Shop shop = shopService.getById(clerk.getShopId());
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @OperationLogRecord(module = "店铺管理", action = "修改店铺资料")
    public Result updateCurrentShop(ClerkShopUpdateDTO updateDTO) {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null) {
            return Result.fail("请先登录店员账号");
        }
        if (!isManager(clerk)) {
            return Result.fail("仅店长可以修改店铺信息");
        }
        if (clerk.getShopId() == null) {
            return Result.fail("当前店员未绑定店铺");
        }
        if (updateDTO == null) {
            return Result.fail("未提交店铺信息");
        }

        Shop currentShop = shopService.getById(clerk.getShopId());
        if (currentShop == null) {
            return Result.fail("店铺不存在");
        }

        String name = StrUtil.trim(updateDTO.getName());
        String area = StrUtil.trim(updateDTO.getArea());
        String address = StrUtil.trim(updateDTO.getAddress());
        String openHours = StrUtil.trim(updateDTO.getOpenHours());
        String images = normalizeImages(updateDTO.getImages());

        if (StrUtil.isBlank(name)) {
            return Result.fail("店铺名称不能为空");
        }
        if (StrUtil.isBlank(address)) {
            return Result.fail("店铺地址不能为空");
        }
        if (StrUtil.isBlank(openHours)) {
            return Result.fail("营业时间不能为空");
        }
        if (updateDTO.getAvgPrice() == null || updateDTO.getAvgPrice() < 0) {
            return Result.fail("请输入正确的人均价格");
        }

        Shop shop = new Shop();
        shop.setId(currentShop.getId());
        shop.setName(name);
        shop.setArea(area);
        shop.setAddress(address);
        shop.setAvgPrice(updateDTO.getAvgPrice());
        shop.setOpenHours(openHours);
        shop.setImages(StrUtil.isBlank(images) ? currentShop.getImages() : images);
        return shopService.updateShop(shop);
    }

    @Override
    public Result queryMyStaff() {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null) {
            return Result.fail("请先登录店员账号");
        }
        if (!isManager(clerk)) {
            return Result.fail("仅店长可以查看本店店员");
        }

        List<ClerkStaffVO> staffList = lambdaQuery()
                .eq(ShopClerk::getShopId, clerk.getShopId())
                .orderByAsc(ShopClerk::getRole)
                .orderByAsc(ShopClerk::getId)
                .list()
                .stream()
                .map(item -> BeanUtil.copyProperties(item, ClerkStaffVO.class))
                .collect(Collectors.toList());
        return Result.ok(staffList);
    }

    @Override
    @OperationLogRecord(module = "员工管理", action = "创建员工")
    public Result createMyStaff(ClerkStaffSaveDTO saveDTO) {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null) {
            return Result.fail("请先登录店长账号");
        }
        if (!isManager(clerk)) {
            return Result.fail("仅店长可以创建本店员工");
        }

        String username = saveDTO.getUsername().trim();
        String redisKey = RedisConstants.IDEMPOTENT_CREATE_STAFF_KEY + clerk.getShopId() + ":" + username;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, String.valueOf(clerk.getId()), 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("请勿重复提交");
        }

        ShopClerk newClerk = null;
        try {
            long exists = lambdaQuery()
                    .eq(ShopClerk::getUsername, username)
                    .count();
            if (exists > 0) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("店员账号已存在");
            }

            newClerk = new ShopClerk();
            newClerk.setShopId(clerk.getShopId());
            newClerk.setUsername(username);
            newClerk.setPassword(saveDTO.getPassword().trim());
            newClerk.setName(saveDTO.getName().trim());
            newClerk.setRole(ROLE_STAFF);
            newClerk.setStatus(1);
            save(newClerk);
        } catch (Exception e) {
            log.error("创建员工失败, shopId={}, username={}", clerk.getShopId(), username, e);
            stringRedisTemplate.delete(redisKey);
            throw e;
        }
        return Result.ok(BeanUtil.copyProperties(newClerk, ClerkStaffVO.class));
    }

    @Override
    public Result changePassword(ChangePasswordDTO dto) {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null || clerk.getId() == null) {
            return Result.fail("请先登录店员账号");
        }

        ShopClerk latest = getById(clerk.getId());

        String oldPassword = dto.getOldPassword().trim();
        String newPassword = dto.getNewPassword().trim();

        if (!oldPassword.equals(latest.getPassword())) {
            return Result.fail("旧密码错误");
        }
        if (newPassword.equals(oldPassword)) {
            return Result.fail("新密码不能与旧密码一致");
        }

        ShopClerk updateClerk = new ShopClerk();
        updateClerk.setId(latest.getId());
        updateClerk.setPassword(newPassword);
        if (!updateById(updateClerk)) {
            return Result.fail("密码修改失败，请稍后重试");
        }

        clearCurrentLoginToken();
        ClerkHolder.removeClerk();
        return Result.ok();
    }

    private String normalizeImages(String images) {
        if (StrUtil.isBlank(images)) {
            return null;
        }
        String normalized = images
                .replace("\r", "\n")
                .replace("，", ",")
                .replace(";", ",")
                .replace("\n", ",");
        return StrUtil.join(",", StrUtil.splitTrim(normalized, ','));
    }

    private boolean isManager(ClerkDTO clerk) {
        return Integer.valueOf(ROLE_MANAGER).equals(clerk.getRole());
    }

    private void clearCurrentLoginToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return;
        }
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return;
        }
        stringRedisTemplate.delete(token);
    }
}
