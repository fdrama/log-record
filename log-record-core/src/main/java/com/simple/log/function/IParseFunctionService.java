package com.simple.log.function;


/**
 * @author fdrama
 */
public interface IParseFunctionService {

    /**
     * 方法执行
     * @param functionName
     * @param values
     * @return
     */
    String apply(String functionName, Object... values);

    /**
     * 是否在业务方法执行前执行
     *
     * @param functionName
     * @return
     */
    boolean beforeFunction(String functionName);
}
