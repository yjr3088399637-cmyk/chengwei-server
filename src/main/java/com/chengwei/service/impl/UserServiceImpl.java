package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.LoginFormDTO;
import com.chengwei.dto.Result;
import com.chengwei.dto.SetPasswordDTO;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.User;
import com.chengwei.mapper.UserMapper;
import com.chengwei.service.IUserService;
import com.chengwei.utils.PasswordEncoder;
import com.chengwei.utils.RedisConstants;
import com.chengwei.utils.RegexUtils;
import com.chengwei.utils.UserHolder;
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

import static com.chengwei.utils.SystemConstants.DEFAULT_USER_ICON;
import static com.chengwei.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set("login:code:" + phone, code, 2, TimeUnit.MINUTES);
        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        if (StrUtil.isNotBlank(loginForm.getPassword())) {
            return loginByPassword(loginForm);
        }
        return loginByCode(loginForm);
    }

    private Result loginByCode(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        if (StrUtil.isBlank(code)) {
            return Result.fail("验证码不能为空");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get("login:code:" + phone);
        if (!code.equals(cacheCode)) {
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
            user.setIcon(DEFAULT_USER_ICON);
            save(user);
        }
        return Result.ok(createLoginToken(user));
    }

    private Result loginByPassword(LoginFormDTO loginForm) {
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            return Result.fail("账号不存在");
        }
        if (StrUtil.isBlank(user.getPassword())) {
            return Result.fail("当前账号尚未设置密码");
        }
        if (!PasswordEncoder.matches(user.getPassword(), loginForm.getPassword())) {
            return Result.fail("手机号或密码错误");
        }
        return Result.ok(createLoginToken(user));
    }

    private String createLoginToken(User user) {
        String token = "login:token:" + UUID.randomUUID();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(token, map);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);
        return token;
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

        User latestUser = getById(currentUser.getId());
        syncLoginUserCache(latestUser != null ? latestUser : updateUser, currentUser);
        return Result.ok();
    }

    @Override
    public Result passwordStatus() {
        User currentUser = getCurrentUserEntity();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("hasPassword", StrUtil.isNotBlank(currentUser.getPassword()));
        return Result.ok(data);
    }

    @Override
    public Result setPassword(SetPasswordDTO dto) {
        User currentUser = getCurrentUserEntity();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        if (dto == null || StrUtil.isBlank(dto.getPassword())) {
            return Result.fail("新密码不能为空");
        }
        if (StrUtil.isNotBlank(currentUser.getPassword())) {
            return Result.fail("当前账号已设置密码，请走修改密码流程");
        }
        String password = dto.getPassword().trim();
        if (!isPasswordValid(password)) {
            return Result.fail("密码需为 6-20 位");
        }
        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setPassword(PasswordEncoder.encode(password));
        updateById(updateUser);
        return Result.ok();
    }

    @Override
    public Result changePassword(ChangePasswordDTO dto) {
        User currentUser = getCurrentUserEntity();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        if (dto == null) {
            return Result.fail("参数错误");
        }
        if (StrUtil.isBlank(currentUser.getPassword())) {
            return Result.fail("当前账号尚未设置密码");
        }
        if (StrUtil.isBlank(dto.getOldPassword()) || StrUtil.isBlank(dto.getNewPassword())) {
            return Result.fail("旧密码和新密码都不能为空");
        }
        if (!PasswordEncoder.matches(currentUser.getPassword(), dto.getOldPassword())) {
            return Result.fail("旧密码错误");
        }
        String newPassword = dto.getNewPassword().trim();
        if (!isPasswordValid(newPassword)) {
            return Result.fail("密码需为 6-20 位");
        }
        if (PasswordEncoder.matches(currentUser.getPassword(), newPassword)) {
            return Result.fail("新密码不能与旧密码一致");
        }
        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setPassword(PasswordEncoder.encode(newPassword));
        updateById(updateUser);
        clearCurrentLoginToken();
        UserHolder.removeUser();
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

    private User getCurrentUserEntity() {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        return getById(currentUser.getId());
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

    private boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6 && password.length() <= 20;
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
