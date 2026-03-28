package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set("login:code:"+phone,code,2,TimeUnit.MINUTES);
        log.debug("发送短信验证码成功，验证码：{}",code);
        return null;
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }
        if(code.isEmpty()){
            return Result.fail("验证码为空");
        }
        //此处进行验证码校验
        if(!code.equals(stringRedisTemplate.opsForValue().get("login:code:"+phone))){
            return Result.fail("验证码错误");
        }
        //根据手机号查询/创建user
        User user = query().eq("phone",loginForm.getPhone()).one();
        if(user == null){
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName("user_"+RandomUtil.randomNumbers(10));
            save(user);
        }
        //创建token
        String token = "login:token:"+UUID.randomUUID().toString();
        //转为userDTO
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //转map时将userDTO的值转为String类型
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //存进redis
        stringRedisTemplate.opsForHash().putAll(token,map);
        //设置过期时间
        stringRedisTemplate.expire(token,30,TimeUnit.MINUTES);

        return Result.ok(token);

    }
}
