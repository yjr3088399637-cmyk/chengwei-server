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
@TableName("tb_shop_comment")
@Schema(description = "店铺评论")
public class ShopComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "评论 ID", example = "4001")
    private Long id;

    @Schema(description = "店铺 ID", example = "10")
    private Long shopId;

    @Schema(description = "评论用户 ID", example = "1010")
    private Long userId;

    @Schema(description = "评论内容", example = "环境不错，和朋友一起很开心。")
    private String content;

    @Schema(description = "评分，通常 1-5", example = "5")
    private Integer score;

    @Schema(description = "评论图片，多个链接用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    private String images;

    private Integer liked;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private String icon;
}
