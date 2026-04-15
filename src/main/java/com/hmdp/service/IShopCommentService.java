package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopComment;

public interface IShopCommentService extends IService<ShopComment> {

    Result saveComment(ShopComment shopComment);

    Result queryShopComments(Long shopId, Integer current);

    Result deleteComment(Long id);
}
