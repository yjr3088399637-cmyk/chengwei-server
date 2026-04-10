package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aliyun.oss.common.utils.StringUtils;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final IFollowService followService;

    @Override
    public Result queryBlogById(Long BlogId) {
        // 1. 查询blog
        Blog blog = getById(BlogId);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2. 查询blog有关的用户
        saveBlogUserInfo(blog);
        //设置当前用户是否点赞
        setIsLike(blog);
// 3. 返回结果
        return Result.ok(blog);
    }

    private void saveBlogUserInfo(Blog blog) {
        // 2. 查询blog有关的用户
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
    }

    /**
     * 集合是这样的
     *
     * @param blog 博客
     */
    private void setIsLike(Blog blog) {

        String blogId = blog.getId().toString();
        //当前用户是否点赞
        UserDTO currentUser = UserHolder.getUser();
        //如果未登录
        if (currentUser == null) {
            blog.setIsLike(false);
        } else {
            String blogKey = RedisConstants.BLOG_LIKED_KEY + blogId;
            String currentUserId = currentUser.getId().toString();
            Double score = stringRedisTemplate.opsForZSet().score(blogKey, currentUserId);
            blog.setIsLike(score ==null ? false : true);
        }
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
        if (stringSet == null || stringSet.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = stringSet.stream().map(string -> Long.valueOf(string)).collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOList = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("博客保存失败!");
        }
        List<Follow> follows = followService.query().eq("follow_user_id", userId).list();
        //为每个粉丝做博客推送
        follows.forEach(follow -> {
            Long fansId  = follow.getUserId();
            String key = RedisConstants.FEED_KEY + fansId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        });
        return Result.ok(blog.getId());
    }



    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;

        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate
                .opsForZSet().reverseRangeByScoreWithScores(key, max, 0, offset, 2);

        //和上一次分页的偏移量
        Integer returnOffset = 1;
        //本次分页的最小时间戳
        Long minTime = 0L;

        //非空判断
        if(typedTuples == null ||typedTuples.isEmpty()) {
            return Result.ok();
        }
        List<Long> ids = new ArrayList<>();
        //按照时间戳从大到小遍历
        for( ZSetOperations.TypedTuple<String> typedTuple: typedTuples){
            //添加用户id
            String id = typedTuple.getValue();
            ids.add(Long.valueOf(id));

            Long score = typedTuple.getScore().longValue();
            //计算minTime,returnOffset值
            if(score.equals(minTime)){
                returnOffset++;
            }else{
                minTime = score;
                returnOffset = 1;
            }

        }
        String idsStr = StringUtils.join(String.valueOf(ids),",");
        List<Blog> blogList = query().in("id", ids).last("ORDER BY FIELD ( id" + idsStr + ")").list();
        blogList.forEach(blog -> {
            //完善信息
            saveBlogUserInfo(blog);
            setIsLike(blog);
        });
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setOffset(returnOffset);
        scrollResult.setMinTime(minTime);

        return Result.ok(scrollResult);
    }
}
