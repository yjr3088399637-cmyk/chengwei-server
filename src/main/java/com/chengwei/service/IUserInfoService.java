package com.chengwei.service;

import com.chengwei.dto.Result;
import com.chengwei.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @since 2021-12-24
 */
public interface IUserInfoService extends IService<UserInfo> {

    Result queryMyInfo();

    Result updateMyInfo(UserInfo userInfo);
}
