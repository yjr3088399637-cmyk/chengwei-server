package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    @Resource
    private IBlogCommentsService blogCommentsService;

    @PostMapping
    public Result saveComment(@RequestBody BlogComments blogComments) {
        return blogCommentsService.saveComment(blogComments);
    }

    @GetMapping("/of/blog/{id}")
    public Result queryBlogComments(
            @PathVariable("id") Long blogId,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogCommentsService.queryBlogComments(blogId, current);
    }
}
