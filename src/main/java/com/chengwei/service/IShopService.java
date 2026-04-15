package com.chengwei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chengwei.dto.Result;
import com.chengwei.entity.Shop;

public interface IShopService extends IService<Shop> {

    Result qurryById(Long id);

    Result updateShop(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, String sortBy, Double x, Double y);
}
