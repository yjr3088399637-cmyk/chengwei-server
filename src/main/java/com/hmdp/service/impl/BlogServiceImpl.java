package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    private final IUserService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final IFollowService followService;
    private final BlogCommentsMapper blogCommentsMapper;

    @Override
    public Result queryBlogById(Long blogId) {
        Blog blog = getById(blogId);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        saveBlogUserInfo(blog);
        setIsLike(blog);
        return Result.ok(blog);
    }

    private void saveBlogUserInfo(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
    }

    private void setIsLike(Blog blog) {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            blog.setIsLike(false);
            return;
        }
        String blogKey = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        String currentUserId = currentUser.getId().toString();
        Double score = stringRedisTemplate.opsForZSet().score(blogKey, currentUserId);
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long blogId) {
        String strUserId = UserHolder.getUser().getId().toString();
        String blogKey = RedisConstants.BLOG_LIKED_KEY + blogId;

        Double score = stringRedisTemplate.opsForZSet().score(blogKey, strUserId);
        if (score != null) {
            boolean success = update()
                    .setSql("liked = liked - 1")
                    .eq("id", blogId)
                    .update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(blogKey, strUserId);
            }
        } else {
            boolean success = update()
                    .setSql("liked = liked + 1")
                    .eq("id", blogId)
                    .update();
            if (success) {
                stringRedisTemplate.opsForZSet().add(blogKey, strUserId, System.currentTimeMillis());
            }
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
        List<Long> ids = stringSet.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOList = users.stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        boolean success = save(blog);
        if (!success) {
            return Result.fail("笔记保存失败");
        }
        List<Follow> follows = followService.query().eq("follow_user_id", userId).list();
        follows.forEach(follow -> {
            Long fansId = follow.getUserId();
            String key = RedisConstants.FEED_KEY + fansId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        });
        return Result.ok(blog.getId());
    }

    @Override
    public Result updateBlog(Blog blog) {
        if (blog == null || blog.getId() == null) {
            return Result.fail("笔记不存在");
        }
        if (StrUtil.isBlank(blog.getTitle())) {
            return Result.fail("标题不能为空");
        }
        if (StrUtil.isBlank(blog.getContent())) {
            return Result.fail("内容不能为空");
        }
        if (blog.getShopId() == null) {
            return Result.fail("请选择关联商户");
        }
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        Blog existed = getById(blog.getId());
        if (existed == null) {
            return Result.fail("笔记不存在");
        }
        if (!currentUser.getId().equals(existed.getUserId())) {
            return Result.fail("无权限编辑该笔记");
        }

        Blog updateBlog = new Blog();
        updateBlog.setId(existed.getId());
        updateBlog.setShopId(blog.getShopId());
        updateBlog.setTitle(StrUtil.trim(blog.getTitle()));
        updateBlog.setContent(StrUtil.trim(blog.getContent()));
        updateBlog.setImages(StrUtil.blankToDefault(StrUtil.trim(blog.getImages()), ""));
        boolean success = updateById(updateBlog);
        return success ? Result.ok(updateBlog.getId()) : Result.fail("笔记更新失败");
    }

    @Override
    @Transactional
    public Result deleteBlog(Long id) {
        if (id == null) {
            return Result.fail("笔记不存在");
        }
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        Blog existed = getById(id);
        if (existed == null) {
            return Result.fail("笔记不存在");
        }
        if (!currentUser.getId().equals(existed.getUserId())) {
            return Result.fail("无权限删除该笔记");
        }

        boolean removed = removeById(id);
        if (!removed) {
            return Result.fail("删除笔记失败");
        }

        blogCommentsMapper.delete(Wrappers.<BlogComments>lambdaQuery().eq(BlogComments::getBlogId, id));
        stringRedisTemplate.delete(RedisConstants.BLOG_LIKED_KEY + id);

        List<Follow> follows = followService.query()
                .eq("follow_user_id", existed.getUserId())
                .list();
        if (CollUtil.isNotEmpty(follows)) {
            follows.forEach(follow -> stringRedisTemplate.opsForZSet().remove(
                    RedisConstants.FEED_KEY + follow.getUserId(),
                    id.toString()
            ));
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;

        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate
                .opsForZSet().reverseRangeByScoreWithScores(key, max, 0, offset, 2);

        Integer returnOffset = 1;
        Long minTime = 0L;

        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        List<Long> ids = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String id = typedTuple.getValue();
            ids.add(Long.valueOf(id));

            Long score = typedTuple.getScore().longValue();
            if (score.equals(minTime)) {
                returnOffset++;
            } else {
                minTime = score;
                returnOffset = 1;
            }
        }
        String idsStr = StringUtils.join(String.valueOf(ids), ",");
        List<Blog> blogList = query().in("id", ids).last("ORDER BY FIELD ( id" + idsStr + ")").list();
        blogList.forEach(blog -> {
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
