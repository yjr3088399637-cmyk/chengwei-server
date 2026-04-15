package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.ChangePasswordDTO;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.SetPasswordDTO;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result updateMyProfile(User user);

    Result passwordStatus();

    Result setPassword(SetPasswordDTO dto);

    Result changePassword(ChangePasswordDTO dto);

    Result sign();

    Result signCount();
}
