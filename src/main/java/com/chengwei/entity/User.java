package com.chengwei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
@Schema(description = "用户基础信息")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户 ID", example = "1010")
    private Long id;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "18909233524")
    private String phone;

    /**
     * 密码，已加密存储
     */
    @Schema(description = "登录密码", example = "123456")
    private String password;

    /**
     * 昵称，默认是随机字符
     */
    @Schema(description = "昵称", example = "小可爱")
    @Size(max = 32, message = "昵称长度不能超过 32 个字符")
    private String nickName;

    /**
     * 用户头像
     */
    @Schema(description = "头像链接", example = "https://example.com/avatar.jpg")
    @Size(max = 255, message = "头像链接长度不能超过 255 个字符")
    private String icon;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
