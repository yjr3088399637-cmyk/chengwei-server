package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
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
        if (userInfo == null) {
            return Result.fail("参数错误");
        }

        UserInfo dbInfo = getById(user.getId());
        if (dbInfo == null) {
            dbInfo = new UserInfo();
            dbInfo.setUserId(user.getId());
        }

        if (userInfo.getIntroduce() != null) {
            String introduce = StrUtil.trim(userInfo.getIntroduce());
            if (introduce.length() > 128) {
                return Result.fail("个人介绍不能超过128个字符");
            }
            dbInfo.setIntroduce(StrUtil.emptyToNull(introduce));
        }
        if (userInfo.getCity() != null) {
            String city = StrUtil.trim(userInfo.getCity());
            if (city.length() > 64) {
                return Result.fail("城市名称不能超过64个字符");
            }
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
