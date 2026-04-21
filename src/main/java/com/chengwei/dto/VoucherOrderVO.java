package com.chengwei.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "订单展示对象")
public class VoucherOrderVO {
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "订单 ID", example = "38927852699123725")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "优惠券 ID", example = "11")
    private Long voucherId;

    @Schema(description = "核销门店 ID", example = "10")
    private Long shopId;

    @Schema(description = "门店名称", example = "开乐迪KTV（运河上街店）")
    private String shopName;

    @Schema(description = "下单用户昵称", example = "小可爱")
    private String userNickName;

    @Schema(description = "下单用户手机号", example = "18909233524")
    private String userPhone;

    @Schema(description = "核销码", example = "CWANAKM6BXIQ3")
    private String verifyCode;

    @Schema(description = "核销店员名称", example = "2号店员")
    private String verifyClerkName;

    @Schema(description = "优惠券标题", example = "100元代金券")
    private String voucherTitle;

    @Schema(description = "优惠券副标题", example = "周一至周五均可使用")
    private String voucherSubTitle;

    @Schema(description = "支付金额，单位分", example = "8000")
    private Long payValue;

    @Schema(description = "抵扣金额，单位分", example = "10000")
    private Long actualValue;

    @Schema(description = "订单状态，1待支付 2已支付 3已核销 4已取消", example = "2")
    private Integer status;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "核销时间")
    private LocalDateTime useTime;
}
