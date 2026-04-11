package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;

public interface IBlogCommentsService extends IService<BlogComments> {

    Result saveComment(BlogComments blogComments);

    Result queryBlogComments(Long blogId, Integer current);
}
