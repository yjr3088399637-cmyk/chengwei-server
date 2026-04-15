package com.chengwei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chengwei.dto.Result;
import com.chengwei.entity.ShopComment;

public interface IShopCommentService extends IService<ShopComment> {

    Result saveComment(ShopComment shopComment);

    Result queryShopComments(Long shopId, Integer current);

    Result deleteComment(Long id);
}
