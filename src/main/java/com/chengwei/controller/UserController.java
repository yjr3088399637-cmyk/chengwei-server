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
import com.chengwei.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    @PutMapping("/me")
    public Result updateMe(@RequestBody User user) {
        return userService.updateMyProfile(user);
    }

    @GetMapping("/password/status")
    public Result passwordStatus() {
        return userService.passwordStatus();
    }

    @PostMapping("/password/set")
    public Result setPassword(@RequestBody SetPasswordDTO dto) {
        return userService.setPassword(dto);
    }

    @PutMapping("/password/change")
    public Result changePassword(@RequestBody ChangePasswordDTO dto) {
        return userService.changePassword(dto);
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (token != null && !token.isEmpty()) {
            stringRedisTemplate.delete(token);
        }
        UserHolder.removeUser();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }

    @GetMapping("/info/{id}")
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
    public Result myInfo() {
        return userInfoService.queryMyInfo();
    }

    @PutMapping("/info")
    public Result updateMyInfo(@RequestBody UserInfo userInfo) {
        return userInfoService.updateMyInfo(userInfo);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }

    @PostMapping("/signCount")
    public Result signCount() {
        return userService.signCount();
    }
}
