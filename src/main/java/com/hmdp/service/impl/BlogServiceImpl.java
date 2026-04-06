package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    private final IUserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        // 1. 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
// 2. 查询blog有关的用户
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
        //当前用户是否点赞
        UserDTO currentUser = UserHolder.getUser();
        //如果未登录
        if (currentUser == null) {
            blog.setIsLike(false);
        } else {
            String blogKey = RedisConstants.BLOG_LIKED_KEY + id;
            String currentUserId = currentUser.getId().toString();
            Double score = stringRedisTemplate.opsForZSet().score(blogKey, currentUserId);
            blog.setIsLike(score ==null ? false : true);
        }
// 3. 返回结果
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long blogId) {
        String strUserId = UserHolder.getUser().getId().toString();
        String blogKey = RedisConstants.BLOG_LIKED_KEY + blogId;

        //判断用户是否点赞
        Double score = stringRedisTemplate.opsForZSet().score(blogKey, strUserId);
        if (score != null) {
            boolean success = update()
                    .setSql("liked = liked - 1")
                    .eq("id", blogId)
                    .update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(blogKey, strUserId);
            }
        }else{
            boolean success = update()
                    .setSql("liked = liked + 1")
                    .eq("id", blogId)
                    .update();
            stringRedisTemplate.opsForZSet().add(blogKey,strUserId,System.currentTimeMillis());
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String blogKey = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> stringSet = stringRedisTemplate.opsForZSet().range(blogKey, 0, 4);
        if (stringSet == null) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = stringSet.stream().map(string -> Long.valueOf(string)).collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOList = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

}
