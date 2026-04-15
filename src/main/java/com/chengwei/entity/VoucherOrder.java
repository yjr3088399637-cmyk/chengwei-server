package com.chengwei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 下单用户 id
     */
    private Long userId;

    /**
     * 购买的代金券 id
     */
    private Long voucherId;

    /**
     * 核销码
     */
    private String verifyCode;

    /**
     * 支付方式：1 余额支付，2 支付宝，3 微信
     */
    private Integer payType;

    /**
     * 订单状态：1 未支付，2 已支付，3 已核销，4 已取消，5 退款中，6 已退款
     */
    private Integer status;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 核销时间
     */
    private LocalDateTime useTime;

    /**
     * 核销店员 id
     */
    private Long verifyClerkId;

    /**
     * 核销门店 id
     */
    private Long verifyShopId;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
