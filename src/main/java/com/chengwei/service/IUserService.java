package com.chengwei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.LoginFormDTO;
import com.chengwei.dto.Result;
import com.chengwei.dto.SetPasswordDTO;
import com.chengwei.entity.User;

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
