package com.chengwei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chengwei.dto.Result;
import com.chengwei.entity.BlogComments;

public interface IBlogCommentsService extends IService<BlogComments> {

    Result saveComment(BlogComments blogComments);

    Result queryBlogComments(Long blogId, Integer current);

    Result deleteComment(Long id);
}
