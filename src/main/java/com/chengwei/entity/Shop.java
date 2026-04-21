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
    private String name;

    /**
     * 商铺类型的id
     */
    @Schema(description = "店铺分类 ID", example = "2")
    private Long typeId;

    /**
     * 商铺图片，多个图片以','隔开
     */
    @Schema(description = "店铺图片，多个链接用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    private String images;

    /**
     * 商圈，例如陆家嘴
     */
    @Schema(description = "商圈", example = "运河上街")
    private String area;

    /**
     * 地址
     */
    @Schema(description = "店铺地址", example = "台州路2号运河上街购物中心F4")
    private String address;

    /**
     * 经度
     */
    @Schema(description = "经度", example = "120.128958")
    private Double x;

    /**
     * 维度
     */
    @Schema(description = "纬度", example = "30.337252")
    private Double y;

    /**
     * 均价，取整数
     */
    @Schema(description = "人均价格", example = "80")
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
