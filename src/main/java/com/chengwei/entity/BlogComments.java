package com.chengwei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog_comments")
@Schema(description = "博客评论或回复")
public class BlogComments implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "评论 ID", example = "3001")
    private Long id;

    @Schema(description = "评论用户 ID", example = "1010")
    private Long userId;

    @Schema(description = "所属博客 ID", example = "2001")
    private Long blogId;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private String icon;

    @Schema(description = "父评论 ID，一级评论可为空", example = "3000")
    private Long parentId;

    @Schema(description = "被回复用户 ID，可为空", example = "1001")
    private Long answerId;

    @Schema(description = "评论内容", example = "这家店我也去过，确实不错。")
    private String content;

    private Integer liked;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
