package com.chengwei.service;

import com.chengwei.dto.Result;
import com.chengwei.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
