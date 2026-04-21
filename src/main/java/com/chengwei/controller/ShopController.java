package com.chengwei.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengwei.dto.Result;
import com.chengwei.entity.Shop;
import com.chengwei.service.IShopService;
import com.chengwei.utils.common.SystemConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;

@RestController
@RequestMapping("/shop")
@Tag(name = "公共-店铺模块", description = "店铺详情、分类查询、搜索与附近排序")
public class ShopController {

    @Resource
    public IShopService shopService;

    @GetMapping("/{id}")
    @Operation(summary = "查询店铺详情")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.qurryById(id);
    }

    @PostMapping
    @Operation(summary = "旧公共新增店铺入口（已禁用）")
    public Result saveShop(@RequestBody Shop shop) {
        return Result.fail("请使用管理端新增店铺接口");
    }

    @PutMapping
    @Operation(summary = "旧公共修改店铺入口（已禁用）")
    public Result updateShop(@RequestBody Shop shop) {
        return Result.fail("请使用管理端或店长端的店铺编辑接口");
    }

    @GetMapping("/of/type")
    @Operation(summary = "按分类查询店铺", description = "支持分页、排序、坐标距离查询")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        return shopService.queryShopByType(typeId, current, sortBy, x, y);
    }

    @GetMapping("/of/name")
    @Operation(summary = "按名称搜索店铺", description = "支持按名称、商圈、地址和分类联合搜索")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "typeId", required = false) Long typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "sortBy", required = false) String sortBy
    ) {
        if (StrUtil.isBlank(name)) {
            Page<Shop> page = shopService.query()
                    .eq(typeId != null, "type_id", typeId)
                    .orderByAsc("id")
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        Page<Shop> page = shopService.query()
                .eq(typeId != null, "type_id", typeId)
                .like("name", name)
                .orderByDesc("comments".equals(sortBy), "comments")
                .orderByDesc("score".equals(sortBy), "score")
                .orderByAsc("id")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            page = shopService.query()
                    .eq(typeId != null, "type_id", typeId)
                    .and(wrapper -> wrapper
                            .like("area", name)
                            .or()
                            .like("address", name))
                    .orderByDesc("comments".equals(sortBy), "comments")
                    .orderByDesc("score".equals(sortBy), "score")
                    .orderByAsc("id")
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        }
        return Result.ok(page.getRecords());
    }
}
