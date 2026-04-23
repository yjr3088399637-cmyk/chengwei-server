package com.chengwei.service;

import com.chengwei.dto.Result;
import com.chengwei.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IUserInfoService extends IService<UserInfo> {

    Result queryMyInfo();

    Result updateMyInfo(UserInfo userInfo);
}
