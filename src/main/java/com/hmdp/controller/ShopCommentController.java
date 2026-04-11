package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopComment;
import com.hmdp.service.IShopCommentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop-comments")
public class ShopCommentController {

    @Resource
    private IShopCommentService shopCommentService;

    @PostMapping
    public Result saveComment(@RequestBody ShopComment shopComment) {
        return shopCommentService.saveComment(shopComment);
    }

    @GetMapping("/of/shop/{id}")
    public Result queryShopComments(
            @PathVariable("id") Long shopId,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return shopCommentService.queryShopComments(shopId, current);
    }
}
