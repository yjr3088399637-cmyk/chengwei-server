package com.chengwei.controller;

import com.chengwei.dto.Result;
import com.chengwei.entity.ShopComment;
import com.chengwei.service.IShopCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop-comments")
@Tag(name = "用户端-店铺评论模块", description = "店铺评论发布、删除与列表查询")
public class ShopCommentController {

    @Resource
    private IShopCommentService shopCommentService;

    @PostMapping
    @Operation(summary = "发布店铺评论")
    public Result saveComment(@RequestBody ShopComment shopComment) {
        return shopCommentService.saveComment(shopComment);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除店铺评论")
    public Result deleteComment(@PathVariable("id") Long id) {
        return shopCommentService.deleteComment(id);
    }

    @GetMapping("/of/shop/{id}")
    @Operation(summary = "查询店铺评论列表")
    public Result queryShopComments(
            @PathVariable("id") Long shopId,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return shopCommentService.queryShopComments(shopId, current);
    }
}
