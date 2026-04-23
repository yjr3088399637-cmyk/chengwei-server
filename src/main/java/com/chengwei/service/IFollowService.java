package com.chengwei.service;

import com.chengwei.dto.Result;
import com.chengwei.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);

    Result queryMyFollows();

    Result queryMyFans();
}
