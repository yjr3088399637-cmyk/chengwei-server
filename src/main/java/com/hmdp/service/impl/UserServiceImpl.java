package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set("login:code:" + phone, code, 2, TimeUnit.MINUTES);
        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号错误");
        }
        if (code == null || code.isEmpty()) {
            return Result.fail("验证码为空");
        }
        if (!code.equals(stringRedisTemplate.opsForValue().get("login:code:" + phone))) {
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName("user_" + RandomUtil.randomNumbers(10));
            save(user);
        }

        String token = "login:token:" + UUID.randomUUID();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(token, map);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    @Override
    public Result updateMyProfile(User user) {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        if (user == null) {
            return Result.fail("参数错误");
        }

        User updateUser = new User();
        updateUser.setId(currentUser.getId());

        if (user.getNickName() != null) {
            String nickName = user.getNickName().trim();
            if (nickName.isEmpty()) {
                return Result.fail("昵称不能为空");
            }
            if (nickName.length() > 32) {
                return Result.fail("昵称不能超过32个字符");
            }
            updateUser.setNickName(nickName);
        }
        if (user.getIcon() != null) {
            updateUser.setIcon(user.getIcon().trim());
        }

        boolean success = updateById(updateUser);
        if (!success) {
            return Result.fail("更新用户信息失败");
        }

        syncLoginUserCache(updateUser, currentUser);
        return Result.ok();
    }

    private void syncLoginUserCache(User updateUser, UserDTO currentUser) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return;
        }
        String token = request.getHeader("authorization");
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        Map<String, Object> cacheMap = new HashMap<>();
        cacheMap.put("id", currentUser.getId().toString());
        cacheMap.put("nickName", updateUser.getNickName() != null ? updateUser.getNickName() : currentUser.getNickName());
        cacheMap.put("icon", updateUser.getIcon() != null ? updateUser.getIcon() : currentUser.getIcon());
        stringRedisTemplate.opsForHash().putAll(token, cacheMap);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);
    }

    @Override
    public Result sign() {
        String userId = UserHolder.getUser().getId().toString();
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM");
        String date = now.format(formatter);
        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + date;
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        String userId = UserHolder.getUser().getId().toString();
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM");
        String date = now.format(formatter);
        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + date;

        List<Long> longList = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (longList == null || longList.isEmpty()) {
            return Result.ok(0);
        }
        Long records = longList.get(0);
        if (records == null || records == 0) {
            return Result.ok(0);
        }

        int count = 0;
        while (true) {
            if ((records & 1) == 0) {
                break;
            }
            count++;
            records >>>= 1;
        }
        return Result.ok(count);
    }
}
