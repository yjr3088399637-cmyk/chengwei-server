package com.chengwei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_operation_log")
public class OperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人类型：1用户 2店员 3店长 4管理员
     */
    private Integer operatorType;

    /**
     * 操作人id
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 业务模块
     */
    private String module;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作目标类型
     */
    private String targetType;

    /**
     * 操作目标id
     */
    private String targetId;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求路径
     */
    private String requestUri;

    /**
     * 请求ip
     */
    private String requestIp;

    /**
     * 是否成功：1成功 0失败
     */
    private Integer success;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 方法执行耗时，单位毫秒
     */
    private Long durationMs;

    private LocalDateTime createTime;
}
