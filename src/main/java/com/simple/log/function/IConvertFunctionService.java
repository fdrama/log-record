package com.simple.log.function;


/**
 * @author fdrama
 */
public interface IConvertFunctionService {

    /**
     * 方法执行
     * @param functionName
     * @param values
     * @return
     */
    String convert(String functionName, Object... values);

    /**
     * 是否在方法执行前执行
     *
     * @param functionName
     * @return
     */
    boolean beforeFunction(String functionName);
}
