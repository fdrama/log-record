package com.simple.log.function.parse;

/**
 * @author fdrama
 */
public enum DefaultParseFunctionEnum {

    /**
     * 默认转换方法
     */
    NOW("_NOW", "获取当前日期"),
    DIFF("_DIFF", "比对两个对象"),

    ;


    private final String functionName;

    private final String desc;

    DefaultParseFunctionEnum(String functionName, String desc) {
        this.functionName = functionName;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String getFunctionName() {
        return functionName;
    }
}
