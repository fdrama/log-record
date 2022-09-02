package com.simple.log.function.convert;

/**
 * @author fdrama
 */
public interface IConvertFunction {

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
    String convert(Object... values);
}
