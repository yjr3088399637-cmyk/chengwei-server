package com.chengwei.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengwei.dto.Result;
import com.chengwei.entity.Shop;
import com.chengwei.service.IShopService;
import com.chengwei.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.qurryById(id);
    }

    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        shopService.save(shop);
        return Result.ok(shop.getId());
    }

    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        return shopService.updateShop(shop);
    }

    @GetMapping("/of/type")
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
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "sortBy", required = false) String sortBy
    ) {
        if (StrUtil.isBlank(name)) {
            Page<Shop> page = shopService.query()
                    .orderByAsc("id")
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        Page<Shop> page = shopService.query()
                .like("name", name)
                .orderByDesc("comments".equals(sortBy), "comments")
                .orderByDesc("score".equals(sortBy), "score")
                .orderByAsc("id")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            page = shopService.query()
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
