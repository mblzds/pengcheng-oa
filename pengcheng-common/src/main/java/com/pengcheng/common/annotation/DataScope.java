package com.pengcheng.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /**
     * 部门表的别名
     */
    String deptAlias() default "";

    /**
     * 用户表的别名
     */
    String userAlias() default "";

    /**
     * 联盟商表的别名（房产业务数据权限）
     */
    String allianceAlias() default "";

    /**
     * 项目表的别名（房产业务数据权限）
     */
    String projectAlias() default "";
}

