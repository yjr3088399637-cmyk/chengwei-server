package com.chengwei.controller;

import cn.hutool.core.bean.BeanUtil;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.LoginFormDTO;
import com.chengwei.dto.Result;
import com.chengwei.dto.SetPasswordDTO;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.User;
import com.chengwei.entity.UserInfo;
import com.chengwei.service.IUserInfoService;
import com.chengwei.service.IUserService;
import com.chengwei.utils.holder.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户端-用户模块", description = "验证码登录、个人资料、密码、签到等接口")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("code")
    @Operation(summary = "发送验证码", description = "根据手机号发送登录验证码")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持验证码或密码登录")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户基础资料", description = "修改昵称、头像等基础字段")
    public Result updateMe(@RequestBody User user) {
        return userService.updateMyProfile(user);
    }

    @GetMapping("/password/status")
    @Operation(summary = "查询密码设置状态")
    public Result passwordStatus() {
        return userService.passwordStatus();
    }

    @PostMapping("/password/set")
    @Operation(summary = "首次设置密码")
    public Result setPassword(@RequestBody SetPasswordDTO dto) {
        return userService.setPassword(dto);
    }

    @PutMapping("/password/change")
    @Operation(summary = "修改登录密码")
    public Result changePassword(@RequestBody ChangePasswordDTO dto) {
        return userService.changePassword(dto);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (token != null && !token.isEmpty()) {
            stringRedisTemplate.delete(token);
        }
        UserHolder.removeUser();
        return Result.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录用户简要信息")
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "查询指定用户扩展资料")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }

    @GetMapping("/info/me")
    @Operation(summary = "查询我的扩展资料")
    public Result myInfo() {
        return userInfoService.queryMyInfo();
    }

    @PutMapping("/info")
    @Operation(summary = "更新我的扩展资料")
    public Result updateMyInfo(@RequestBody UserInfo userInfo) {
        return userInfoService.updateMyInfo(userInfo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询指定用户简要信息")
    public Result queryUserById(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    @Operation(summary = "今日签到")
    public Result sign() {
        return userService.sign();
    }

    @PostMapping("/signCount")
    @Operation(summary = "统计连续签到天数")
    public Result signCount() {
        return userService.signCount();
    }
}
