package com.simple.log.annoation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * @author fdrama
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface LogRecord {
    /**
     * @return 方法执行成功后的日志模版
     */
    String success();

    /**
     * @return 方法执行失败后的日志模版
     */
    String fail() default "";

    /**
     * @return 日志的操作人
     */
    String operator() default "";

    /**
     * @return 操作日志的类型
     */
    String type();

    /**
     * @return 日志的子类型
     */
    String subType() default "";

    /**
     * @return 日志绑定的业务标识
     */
    String businessId() default "";

    /**
     * @return 日志的额外信息
     */
    String extra() default "";

    /**
     * @return 是否记录日志
     */
    String condition() default "";
}
