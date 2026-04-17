package com.chengwei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.ClerkLoginFormDTO;
import com.chengwei.dto.ClerkStaffSaveDTO;
import com.chengwei.dto.ClerkShopUpdateDTO;
import com.chengwei.dto.Result;
import com.chengwei.entity.ShopClerk;

public interface IShopClerkService extends IService<ShopClerk> {
    Result login(ClerkLoginFormDTO loginForm);

    Result me();

    Result queryCurrentShop();

    Result updateCurrentShop(ClerkShopUpdateDTO updateDTO);

    Result queryMyStaff();

    Result createMyStaff(ClerkStaffSaveDTO saveDTO);

    Result changePassword(ChangePasswordDTO dto);
}
