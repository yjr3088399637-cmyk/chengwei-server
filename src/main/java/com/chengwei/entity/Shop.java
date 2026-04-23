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
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop")
@Schema(description = "店铺信息")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "店铺 ID", example = "10")
    private Long id;

    /**
     * 商铺名称
     */
    @Schema(description = "店铺名称", example = "开乐迪KTV（运河上街店）")
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 64, message = "店铺名称长度不能超过 64 个字符")
    private String name;

    /**
     * 商铺类型的id
     */
    @Schema(description = "店铺分类 ID", example = "2")
    @NotNull(message = "店铺分类不能为空")
    private Long typeId;

    /**
     * 商铺图片，多个图片以','隔开
     */
    @Schema(description = "店铺图片，多个链接用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    @Size(max = 2048, message = "图片链接总长度不能超过 2048 个字符")
    private String images;

    /**
     * 商圈，例如陆家嘴
     */
    @Schema(description = "商圈", example = "运河上街")
    @Size(max = 64, message = "商圈长度不能超过 64 个字符")
    private String area;

    /**
     * 地址
     */
    @Schema(description = "店铺地址", example = "台州路2号运河上街购物中心F4")
    @NotBlank(message = "店铺地址不能为空")
    @Size(max = 255, message = "店铺地址长度不能超过 255 个字符")
    private String address;

    /**
     * 经度
     */
    @Schema(description = "经度", example = "120.128958")
    @NotNull(message = "经度不能为空")
    private Double x;

    /**
     * 维度
     */
    @Schema(description = "纬度", example = "30.337252")
    @NotNull(message = "纬度不能为空")
    private Double y;

    /**
     * 均价，取整数
     */
    @Schema(description = "人均价格", example = "80")
    @PositiveOrZero(message = "人均价格不能为负数")
    private Long avgPrice;

    /**
     * 销量
     */
    private Integer sold;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 评分，1~5分，乘10保存，避免小数
     */
    private Integer score;

    /**
     * 营业时间，例如 10:00-22:00
     */
    @Schema(description = "营业时间", example = "10:00-22:00")
    @NotBlank(message = "营业时间不能为空")
    @Size(max = 32, message = "营业时间长度不能超过 32 个字符")
    private String openHours;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private Double distance;
}
