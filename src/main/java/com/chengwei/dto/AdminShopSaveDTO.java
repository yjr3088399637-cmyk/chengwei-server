package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

@Data
@Schema(description = "管理员新增或编辑店铺请求")
public class AdminShopSaveDTO {
    @Schema(description = "店铺名称", example = "城味小馆（万达店）")
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 64, message = "店铺名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "店铺分类 ID", example = "1")
    @NotNull(message = "请选择店铺分类")
    private Long typeId;

    @Schema(description = "店铺图片，多个链接用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    @Size(max = 2048, message = "图片链接总长度不能超过 2048 个字符")
    private String images;

    @Schema(description = "商圈", example = "拱墅万达")
    @Size(max = 64, message = "商圈长度不能超过 64 个字符")
    private String area;

    @Schema(description = "店铺地址", example = "杭行路666号万达广场4层")
    @NotBlank(message = "店铺地址不能为空")
    @Size(max = 255, message = "店铺地址长度不能超过 255 个字符")
    private String address;

    @Schema(description = "经度", example = "120.128958")
    @NotNull(message = "经度不能为空")
    private Double x;

    @Schema(description = "纬度", example = "30.337252")
    @NotNull(message = "纬度不能为空")
    private Double y;

    @Schema(description = "人均价格", example = "88")
    @NotNull(message = "人均价格不能为空")
    @PositiveOrZero(message = "人均价格不能为负数")
    private Long avgPrice;

    @Schema(description = "营业时间", example = "10:00-22:00")
    @NotBlank(message = "营业时间不能为空")
    @Size(max = 32, message = "营业时间长度不能超过 32 个字符")
    private String openHours;

    @Schema(description = "首个店长账号", example = "manager18")
    @NotBlank(message = "营业时间不能为空")
    @Size(max = 32, message = "首个店长账号长度不能超过 32 位")
    private String clerkUsername;

    @Schema(description = "首个店长密码", example = "123456")
    @NotBlank(message = "营业时间不能为空")
    @Size(min = 6, max = 20, message = "首个店长密码长度必须在 6-20 位之间")
    private String clerkPassword;

    @Schema(description = "首个店长名称", example = "18号店长")
    @NotBlank(message = "营业时间不能为空")
    @Size(max = 32, message = "首个店长名称长度不能超过 32 个字符")
    private String clerkName;
}
