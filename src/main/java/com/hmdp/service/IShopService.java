package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

public interface IShopService extends IService<Shop> {

    Result qurryById(Long id);

    Result updateShop(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, String sortBy, Double x, Double y);
}
