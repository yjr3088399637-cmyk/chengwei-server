package com.chengwei.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengwei.dto.Result;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.Blog;
import com.chengwei.entity.User;
import com.chengwei.service.IBlogService;
import com.chengwei.service.IUserService;
import com.chengwei.utils.common.SystemConstants;
import com.chengwei.utils.holder.UserHolder;
import com.chengwei.utils.redis.RedisConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/blog")
@Validated
@Tag(name = "用户端-博客模块", description = "博客发布、编辑、点赞、详情、关注流")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @PostMapping
    @Operation(summary = "发布博客")
    public Result saveBlog(@Valid @RequestBody Blog blog) {
       return blogService.saveBlog(blog);
    }

    @PutMapping
    @Operation(summary = "编辑博客")
    public Result updateBlog(@Valid @RequestBody Blog blog) {
        return blogService.updateBlog(blog);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除博客")
    public Result deleteBlog(@PathVariable("id") Long id) {
        return blogService.deleteBlog(id);
    }

    @PutMapping("/like/{id}")
    @Operation(summary = "点赞或取消点赞博客")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量

        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    @Operation(summary = "查询我的博客列表")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    @Operation(summary = "查询热门博客")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            //判断当前用户是否点赞
            UserDTO currUser = UserHolder.getUser();
            //如果用户已登录
            if(currUser != null){
                String currUserId = currUser.getId().toString();
                String blogKey = RedisConstants.BLOG_LIKED_KEY + blog.getId();
                Double score = stringRedisTemplate.opsForZSet().score(blogKey, currUserId);
                blog.setIsLike(score != null);
            }else{
                blog.setIsLike(false);
            }
        });
        return Result.ok(records);
    }
    //查商铺详细信息
    @GetMapping("/{id}")
    @Operation(summary = "查询博客详情")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    @GetMapping("/likes/{id}")
    @Operation(summary = "查询博客点赞用户列表")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    // BlogController
    @GetMapping("/of/user")
    @Operation(summary = "查询指定用户博客列表")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
    //滚动分页查询关注用户的博客推送
    @GetMapping("/of/follow")
    @Operation(summary = "滚动分页查询关注流博客")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset){
        return blogService.queryBlogOfFollow(max, offset);
    }

}
