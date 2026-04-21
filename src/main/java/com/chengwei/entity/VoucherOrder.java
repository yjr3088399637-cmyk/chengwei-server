package com.chengwei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "优惠券订单")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "订单 ID", example = "38927852699123725")
    private Long id;

    @Schema(description = "下单用户 ID", example = "1010")
    private Long userId;

    @Schema(description = "购买的优惠券 ID", example = "11")
    private Long voucherId;

    @Schema(description = "核销码", example = "CWANAKM6BXIQ3")
    private String verifyCode;

    @Schema(description = "支付方式", example = "1")
    private Integer payType;

    @Schema(description = "订单状态，1待支付 2已支付 3已核销 4已取消 5退款中 6已退款", example = "2")
    private Integer status;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "核销时间")
    private LocalDateTime useTime;

    @Schema(description = "核销店员 ID", example = "2")
    private Long verifyClerkId;

    @Schema(description = "核销门店 ID", example = "10")
    private Long verifyShopId;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
