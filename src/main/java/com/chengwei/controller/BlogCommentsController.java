package com.chengwei.controller;

import com.chengwei.dto.Result;
import com.chengwei.entity.BlogComments;
import com.chengwei.service.IBlogCommentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/blog-comments")
@Validated
@Tag(name = "用户端-博客评论模块", description = "博客评论、回复与评论列表")
public class BlogCommentsController {

    @Resource
    private IBlogCommentsService blogCommentsService;

    @PostMapping
    @Operation(summary = "发布博客评论或回复")
    public Result saveComment(@Valid @RequestBody BlogComments blogComments) {
        return blogCommentsService.saveComment(blogComments);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除博客评论")
    public Result deleteComment(@PathVariable("id") Long id) {
        return blogCommentsService.deleteComment(id);
    }

    @GetMapping("/of/blog/{id}")
    @Operation(summary = "查询某篇博客评论列表")
    public Result queryBlogComments(
            @PathVariable("id") Long blogId,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogCommentsService.queryBlogComments(blogId, current);
    }
}
