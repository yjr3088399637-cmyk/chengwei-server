package com.chengwei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chengwei.dto.AdminClerkSaveDTO;
import com.chengwei.dto.AdminLoginFormDTO;
import com.chengwei.dto.AdminShopSaveDTO;
import com.chengwei.dto.Result;
import com.chengwei.entity.Admin;

public interface IAdminService extends IService<Admin> {
    Result login(AdminLoginFormDTO loginForm);

    Result me();

    Result overview();

    Result queryShops(String keyword);


    Result saveShop(AdminShopSaveDTO saveDTO);


    Result queryClerks(String keyword);

    Result queryShopTypes();
}
