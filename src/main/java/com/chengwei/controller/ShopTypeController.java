package com.chengwei.controller;


import com.chengwei.dto.Result;
import com.chengwei.entity.ShopType;
import com.chengwei.service.IShopTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/shop-type")
@Tag(name = "公共-店铺分类模块", description = "店铺分类列表")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    @Operation(summary = "查询全部店铺分类")
    public Result queryTypeList() {

        return typeService.queryTypeList();
    }
}
