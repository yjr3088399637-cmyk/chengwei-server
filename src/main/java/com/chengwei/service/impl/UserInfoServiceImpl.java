package com.chengwei.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.Result;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.UserInfo;
import com.chengwei.mapper.UserInfoMapper;
import com.chengwei.service.IUserInfoService;
import com.chengwei.utils.holder.UserHolder;
import org.springframework.stereotype.Service;

/**
 * 用户扩展资料服务
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Override
    public Result queryMyInfo() {

        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        //查库
        UserInfo info = getById(user.getId());
        if (info == null) {
            info = new UserInfo();
            info.setUserId(user.getId());
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }

    @Override
    public Result updateMyInfo(UserInfo userInfo) {
        Long id = UserHolder.getUser().getId();
        userInfo.setUserId(id);
        //能否查到登陆用户的身份详细信息
        if (getById(id) == null) {
            save(userInfo);
            return Result.ok();
        }
        updateById(userInfo);
        return Result.ok();
    }
}
