package com.chengwei.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。
 * 第一版只要求标出“属于哪个模块、做了什么动作”，
 * 具体操作者由切面从 Holder 中自动解析。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogRecord {

    /**
     * 业务模块，例如：店铺管理、员工管理、订单管理
     */
    String module();

    /**
     * 操作动作，例如：创建店长、创建员工、核销订单
     */
    String action();
}
