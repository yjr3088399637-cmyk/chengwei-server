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
import com.chengwei.utils.common.RegexUtils;
import com.chengwei.utils.holder.UserHolder;
import com.chengwei.utils.redis.RedisConstants;
import com.chengwei.utils.security.PasswordEncoder;
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

import static com.chengwei.utils.common.SystemConstants.DEFAULT_USER_ICON;
import static com.chengwei.utils.common.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 2.生成6位随机数字验证码
        String code = RandomUtil.randomNumbers(6);
        // 3.将验证码存入Redis，5分钟过期（以手机号为key，实现验证码与手机号绑定）
        stringRedisTemplate.opsForValue().set("login:code:" + phone, code, 5, TimeUnit.MINUTES);
        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号格式
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 2.根据是否携带密码，分发到不同的登录逻辑
        if (StrUtil.isNotBlank(loginForm.getPassword())) {
            return loginByPassword(loginForm);
        }
        return loginByCode(loginForm);
    }

    private Result loginByCode(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        // 1.校验验证码不能为空
        if (StrUtil.isBlank(code)) {
            return Result.fail("验证码不能为空");
        }

        // 2.从Redis获取缓存的验证码（手机号错误时cacheCode为null）
        //这里可能为NULL(手机号错误)
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        // 3.比对验证码是否一致（null与用户输入比较会NPE，所以用equals判断）
        if (!code.equals(cacheCode)) {
            return Result.fail("验证码或手机号错误");
        }
        // 4.根据手机号查询用户，不存在则自动注册（验证码登录的隐式注册逻辑）

        User user = query().eq("phone", phone).one();

        if (user == null) {
            user = new User();
            user.setPhone(phone);
            // 自动生成默认昵称和头像
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
            user.setIcon(DEFAULT_USER_ICON);
            save(user);
        }
        // 5.创建登录token并返回
        //创建token并返回
        return Result.ok(createLoginToken(user));
    }

    private Result loginByPassword(LoginFormDTO loginForm) {
        // 1.根据手机号查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            return Result.fail("账号不存在");
        }
        // 2.比对密码（使用自定义PasswordEncoder，内部为sha256+salt加密）
        if (!PasswordEncoder.matches(user.getPassword(), loginForm.getPassword())) {
            return Result.fail("手机号或密码错误");
        }
        // 4.创建登录token并返回
        return Result.ok(createLoginToken(user));
    }

    /**
     * 创建登录令牌
     *
     * @param user 用户
     * @return {@link String}
     */
    private String createLoginToken(User user) {
        // 1.生成全局唯一token作为Redis的key
        String token = "login:token:" + UUID.randomUUID();
        // 2.将User实体转为UserDTO（脱敏，只保留前端需要的字段）
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 3.将UserDTO转为Map
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        // Redis Hash的value只能是String，因此所有字段值必须toString()
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        stringRedisTemplate.opsForHash().putAll(token, map);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 更新个人基本资料
     *
     * @param user 用户
     * @return {@link Result}
     */
    @Override
    public Result updateMyProfile(User user) {

        // 1.从ThreadLocal中获取当前登录用户上下文
        UserDTO currentUser = UserHolder.getUser();

        // 2.构建更新对象，强制使用当前登录用户的id（防止越权修改他人资料）
        User updateUser = new User();
        // 不信任前端传入的 id，而是强制使用当前登录用户自己的 id，避免越权修改别人资料。
        updateUser.setId(currentUser.getId());

        // 3.逐字段校验合法性后赋值（只更新非null字段，实现部分更新）
        //user其他字段判断合法性后赋值给updateUser,但updateUser部分字段仍可能为空
        if (user.getNickName() != null) {
            String nickName = user.getNickName().trim();
            if (nickName.isEmpty()) {
                return Result.fail("昵称不能为空");
            }
            updateUser.setNickName(nickName);
        }
        if (user.getIcon() != null) {
            updateUser.setIcon(user.getIcon().trim());
        }
        // 4.更新数据库
        boolean success = updateById(updateUser);
        if (!success) {
            return Result.fail("更新用户信息失败");
        }

        // 5.更新成功后同步刷新Redis登录态，保证一致性（syncLoginUserCache内部已有null兜底，直接传updateUser即可）
        syncLoginUserCache(updateUser, currentUser);
        return Result.ok();
    }

    /**
     * 密码状态
     *
     * @return {@link Result}
     */
    @Override
    public Result passwordStatus() {
        // 1.从ThreadLocal获取当前登录用户，并从数据库查询完整实体
        //当前账号是否设置过密码，供前端决定展示设置密码还是修改密码。
        User currentUser = getCurrentUserEntity();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }

        // 2.判断password字段是否非空，返回密码状态
        Map<String, Object> data = new HashMap<>();
        data.put("hasPassword", StrUtil.isNotBlank(currentUser.getPassword()));
        return Result.ok(data);
    }

    /**
     * 设置密码
     *
     * @param dto dto
     * @return {@link Result}
     */
    @Override
    public Result setPassword(SetPasswordDTO dto) {
        // 首次设置密码
        // 1.从数据库获取当前用户完整信息
        User currentUser = getCurrentUserEntity();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        // 2.校验当前账号是否已有密码（已有密码应走修改密码流程）
        if (StrUtil.isNotBlank(currentUser.getPassword())) {
            return Result.fail("当前账号已设置密码，请走修改密码流程");
        }
        // 3.加密密码并更新数据库
        String password = dto.getPassword().trim();
        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setPassword(PasswordEncoder.encode(password));
        updateById(updateUser);

        return Result.ok();
    }

    /**
     * 更改密码
     *
     * @param dto dto
     * @return {@link Result}
     */
    @Override
    public Result changePassword(ChangePasswordDTO dto) {
        // 修改密码：先校验旧密码，成功后清掉当前 token，强制重新登录。
        // 1.从数据库获取当前用户完整信息
        User currentUser = getCurrentUserEntity();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        // 2.校验当前账号是否已设置密码
        if (StrUtil.isBlank(currentUser.getPassword())) {
            return Result.fail("当前账号尚未设置密码");
        }
        // 3.校验旧密码是否正确
        if (!PasswordEncoder.matches(currentUser.getPassword(), dto.getOldPassword())) {
            return Result.fail("旧密码错误");
        }
        // 4.校验新密码不能与旧密码相同
        String newPassword = dto.getNewPassword().trim();
        if (PasswordEncoder.matches(currentUser.getPassword(), newPassword)) {
            return Result.fail("新密码不能与旧密码一致");
        }
        // 5.加密新密码并更新数据库
        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setPassword(PasswordEncoder.encode(newPassword));
        updateById(updateUser);
        // 6.清除登录状态，强制用户重新登录（修改密码后旧会话必须失效）
        //清除登录状态
        clearCurrentLoginToken();

        return Result.ok();
    }

    /**
     * 同步登录用户缓存
     *
     * @param updateUser  更新用户
     * @param currentUser 当前用户
     */
    private void syncLoginUserCache(User updateUser, UserDTO currentUser) {
        // 从上下文拿token,从updateUser和currentUser组合新旧值写入Redis
        // 1.从RequestContextHolder中获取当前请求的token
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
        // 2.组装新的缓存数据：id直接取旧值，nickName/icon用新值覆盖、旧值兜底
        Map<String, Object> cacheMap = new HashMap<>();
        cacheMap.put("id", currentUser.getId().toString());
        //这里如果只改部分值,则用旧值做兜底
        cacheMap.put("nickName", updateUser.getNickName() != null ? updateUser.getNickName() : currentUser.getNickName());
        cacheMap.put("icon", updateUser.getIcon() != null ? updateUser.getIcon() : currentUser.getIcon());
        // 3.写入Redis并刷新过期时间,这里并未删掉token,只是更新了部分值
        stringRedisTemplate.opsForHash().putAll(token, cacheMap);
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);
    }

    private User getCurrentUserEntity() {
        // 从ThreadLocal中获取当前登录用户的DTO，再根据id查数据库获取完整实体
        // UserHolder中拿完整user对象
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        return getById(currentUser.getId());
    }

    /**
     * 清除当前登录令牌
     */
    private void clearCurrentLoginToken() {
        //  从上下文拿token并删除，使当前会话立即失效
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
        // 校验密码长度在6~20位之间
        return password != null && password.length() >= 6 && password.length() <= 20;
    }

    /**
     * 标志
     *
     * @return {@link Result}
     */
    @Override
    public Result sign() {
        // 1.从ThreadLocal获取当前用户id
        String userId = UserHolder.getUser().getId().toString();
        // 2.获取当前日期，构造Redis key（格式：user:sign:userId:yyyy:MM）
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM");
        String date = now.format(formatter);
        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + date;
        // 3.利用Redis BitMap实现签到：将当天的offset位设为1（dayOfMonth-1因为offset从0开始）
        // BitMap优势：一个月31天只需4字节存储，且setBit/bitField操作为O(1)
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    /**
     * 信号数
     *
     * @return {@link Result}
     */
    @Override
    public Result signCount() {
        // 1.从ThreadLocal获取当前用户id，构造Redis key
        String userId = UserHolder.getUser().getId().toString();
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM");
        String date = now.format(formatter);
        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + date;

        // 2.使用BITFIELD命令获取从第1天到当天的所有签到位（unsigned(dayOfMonth)位无符号整数）
        // BITFIELD将BitMap中指定范围的位作为一个十进制数返回，每一位0=未签到，1=已签到
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

        // 3.从最低位（当天）开始，逐位与1做与运算，统计连续签到天数
        // 核心思路：从当天往前倒推，遇到第一个0就终止（表示某天未签到，连续中断）
        int count = 0;
        while (true) {
            // 与1做与运算，判断最低位是否为1（1=已签到，0=未签到）
            if ((records & 1) == 0) {
                break;
            }
            count++;
            // 无符号右移1位，丢弃已判断的最低位，继续判断前一天
            records >>>= 1;
        }
        return Result.ok(count);
    }
}
