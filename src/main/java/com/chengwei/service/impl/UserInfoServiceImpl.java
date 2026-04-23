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
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        UserInfo dbInfo = getById(user.getId());
        if (dbInfo == null) {
            dbInfo = new UserInfo();
            dbInfo.setUserId(user.getId());
        }

        if (userInfo.getIntroduce() != null) {
            String introduce = StrUtil.trim(userInfo.getIntroduce());
            dbInfo.setIntroduce(StrUtil.emptyToNull(introduce));
        }
        if (userInfo.getCity() != null) {
            String city = StrUtil.trim(userInfo.getCity());
            dbInfo.setCity(StrUtil.emptyToNull(city));
        }
        if (userInfo.getGender() != null) {
            dbInfo.setGender(userInfo.getGender());
        }
        if (userInfo.getBirthday() != null) {
            dbInfo.setBirthday(userInfo.getBirthday());
        }

        boolean success = saveOrUpdate(dbInfo);
        return success ? Result.ok() : Result.fail("保存资料失败");
    }
}
