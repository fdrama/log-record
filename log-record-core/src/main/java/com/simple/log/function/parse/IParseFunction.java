package com.simple.log.function.parse;

/**
 * @author fdrama
 */
public interface IParseFunction {

    /**
     * 是否在业务方法前执行
     *
     * @return boolean
     */
    default boolean executeBefore() {
        return false;
    }

    /**
     * 函数名称
     *
     * @return 函数自定义名称
     */
    String functionName();

    /**
     * 方法实现
     *
     * @param values 函数入参
     * @return 方法返回值
     */
    String apply(Object... values);
}
