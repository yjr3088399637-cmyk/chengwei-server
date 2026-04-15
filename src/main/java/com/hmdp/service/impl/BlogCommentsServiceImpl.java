package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    private final IBlogService blogService;
    private final IUserService userService;

    @Override
    @Transactional
    public Result saveComment(BlogComments blogComments) {
        if (blogComments == null || blogComments.getBlogId() == null) {
            return Result.fail("参数错误");
        }
        if (StrUtil.isBlank(blogComments.getContent())) {
            return Result.fail("评论内容不能为空");
        }
        String content = StrUtil.trim(blogComments.getContent());
        if (content.length() > 255) {
            return Result.fail("评论内容过长");
        }
        Blog blog = blogService.getById(blogComments.getBlogId());
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        blogComments.setId(null);
        blogComments.setContent(content);
        blogComments.setUserId(user.getId());
        blogComments.setParentId(blogComments.getParentId() == null ? 0L : blogComments.getParentId());
        blogComments.setAnswerId(blogComments.getAnswerId() == null ? 0L : blogComments.getAnswerId());
        blogComments.setLiked(0);
        blogComments.setStatus(false);

        boolean saved = save(blogComments);
        if (!saved) {
            return Result.fail("发布评论失败");
        }

        updateBlogCommentStats(blogComments.getBlogId());
        return Result.ok();
    }

    @Override
    public Result queryBlogComments(Long blogId, Integer current) {
        if (blogId == null) {
            return Result.fail("参数错误");
        }

        Page<BlogComments> page = query()
                .eq("blog_id", blogId)
                .eq("status", 0)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<BlogComments> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }

        Set<Long> userIds = records.stream().map(BlogComments::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        records.forEach(comment -> {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setName(user.getNickName());
                comment.setIcon(user.getIcon());
            }
        });

        return Result.ok(records, page.getTotal());
    }

    @Override
    @Transactional
    public Result deleteComment(Long id) {
        if (id == null) {
            return Result.fail("评论不存在");
        }
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }

        BlogComments comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        if (!currentUser.getId().equals(comment.getUserId())) {
            return Result.fail("无权限删除该评论");
        }

        boolean removed = removeById(id);
        if (!removed) {
            return Result.fail("删除评论失败");
        }

        updateBlogCommentStats(comment.getBlogId());
        return Result.ok();
    }

    private void updateBlogCommentStats(Long blogId) {
        long commentCount = query()
                .eq("blog_id", blogId)
                .eq("status", 0)
                .count();

        boolean updated = blogService.update()
                .set("comments", commentCount)
                .eq("id", blogId)
                .update();
        if (!updated) {
            throw new RuntimeException("更新笔记评论统计失败");
        }
    }
}
