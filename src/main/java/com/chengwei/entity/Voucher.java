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
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
@Schema(description = "优惠券信息")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "优惠券 ID", example = "11")
    private Long id;

    /**
     * 店铺 id
     */
    @Schema(description = "所属店铺 ID", example = "10")
    @NotNull(message = "所属店铺不能为空")
    private Long shopId;

    /**
     * 代金券标题
     */
    @Schema(description = "券标题", example = "100元代金券")
    @NotBlank(message = "券标题不能为空")
    @Size(max = 64, message = "券标题长度不能超过 64 个字符")
    private String title;

    /**
     * 副标题
     */
    @Schema(description = "副标题", example = "周一至周五均可使用")
    @NotBlank(message = "券副标题不能为空")
    @Size(max = 128, message = "券副标题长度不能超过 128 个字符")
    private String subTitle;

    /**
     * 使用规则
     */
    @Schema(description = "使用规则", example = "每桌仅限使用一张，不可叠加")
    @Size(max = 500, message = "使用规则长度不能超过 500 个字符")
    private String rules;

    /**
     * 支付金额
     */
    @Schema(description = "支付金额，单位分", example = "8000")
    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于 0")
    private Long payValue;

    /**
     * 抵扣金额
     */
    @Schema(description = "抵扣金额，单位分", example = "10000")
    @NotNull(message = "抵扣金额不能为空")
    @Positive(message = "抵扣金额必须大于 0")
    private Long actualValue;

    /**
     * 优惠券类型
     */
    @NotNull(message = "券类型不能为空")
    private Integer type;

    /**
     * 状态
     */
    @NotNull(message = "券状态不能为空")
    private Integer status;

    /**
     * 库存
     */
    @TableField(exist = false)
    @PositiveOrZero(message = "库存不能为负数")
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
