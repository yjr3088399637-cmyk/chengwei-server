package com.chengwei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog")
@Schema(description = "博客或探店笔记")
public class Blog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "博客 ID", example = "2001")
    private Long id;
    /**
     * 商户id
     */
    @Schema(description = "关联商户 ID，可为空", example = "10")
    @NotNull(message = "请选择关联商户")
    private Long shopId;
    /**
     * 用户id
     */
    @Schema(description = "作者用户 ID", example = "1010")
    private Long userId;
    /**
     * 用户图标
     */
    @TableField(exist = false)
    private String icon;
    /**
     * 用户姓名
     */
    @TableField(exist = false)
    private String name;
    /**
     * 是否点赞过了
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * 标题
     */
    @Schema(description = "标题", example = "周末去这家店真的值了")
    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题长度不能超过 64 个字符")
    private String title;

    /**
     * 探店的照片，最多9张，多张以","隔开
     */
    @Schema(description = "图片链接，多个用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    @Size(max = 2048, message = "图片链接总长度不能超过 2048 个字符")
    private String images;

    /**
     * 探店的文字描述
     */
    @Schema(description = "正文内容", example = "环境很舒服，适合朋友一起来。")
    @NotBlank(message = "正文内容不能为空")
    @Size(max = 2000, message = "正文内容长度不能超过 2000 个字符")
    private String content;

    /**
     * 点赞数量
     */
    private Integer liked;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
