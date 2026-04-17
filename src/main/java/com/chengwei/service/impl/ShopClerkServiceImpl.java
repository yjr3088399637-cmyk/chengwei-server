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
import com.chengwei.utils.ClerkHolder;
import lombok.RequiredArgsConstructor;
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
public class ShopClerkServiceImpl extends ServiceImpl<ShopClerkMapper, ShopClerk> implements IShopClerkService {
    private static final int ROLE_MANAGER = 1;
    private static final int ROLE_STAFF = 2;

    private final StringRedisTemplate stringRedisTemplate;
    private final IShopService shopService;

    @Override
    public Result login(ClerkLoginFormDTO loginForm) {
        if (loginForm == null || StrUtil.isBlank(loginForm.getUsername()) || StrUtil.isBlank(loginForm.getPassword())) {
            return Result.fail("请输入店员账号和密码");
        }

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
    public Result createMyStaff(ClerkStaffSaveDTO saveDTO) {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null) {
            return Result.fail("请先登录店员账号");
        }
        if (!isManager(clerk)) {
            return Result.fail("仅店长可以创建本店店员");
        }
        if (saveDTO == null || StrUtil.isBlank(saveDTO.getUsername()) || StrUtil.isBlank(saveDTO.getPassword()) || StrUtil.isBlank(saveDTO.getName())) {
            return Result.fail("请完整填写店员账号、密码和名称");
        }

        long exists = lambdaQuery()
                .eq(ShopClerk::getUsername, saveDTO.getUsername().trim())
                .count();
        if (exists > 0) {
            return Result.fail("店员账号已存在");
        }

        ShopClerk newClerk = new ShopClerk();
        newClerk.setShopId(clerk.getShopId());
        newClerk.setUsername(saveDTO.getUsername().trim());
        newClerk.setPassword(saveDTO.getPassword().trim());
        newClerk.setName(saveDTO.getName().trim());
        newClerk.setRole(ROLE_STAFF);
        newClerk.setStatus(1);
        save(newClerk);
        return Result.ok(BeanUtil.copyProperties(newClerk, ClerkStaffVO.class));
    }

    @Override
    public Result changePassword(ChangePasswordDTO dto) {
        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk == null || clerk.getId() == null) {
            return Result.fail("请先登录店员账号");
        }
        if (dto == null) {
            return Result.fail("参数错误");
        }
        if (StrUtil.isBlank(dto.getOldPassword()) || StrUtil.isBlank(dto.getNewPassword())) {
            return Result.fail("旧密码和新密码都不能为空");
        }

        ShopClerk latest = getById(clerk.getId());
        if (latest == null || !Integer.valueOf(1).equals(latest.getStatus())) {
            return Result.fail("当前店员账号不可用");
        }

        String oldPassword = dto.getOldPassword().trim();
        String newPassword = dto.getNewPassword().trim();
        if (!oldPassword.equals(latest.getPassword())) {
            return Result.fail("旧密码错误");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.fail("密码长度需为 6-20 位");
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
                .replace("；", ",")
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
