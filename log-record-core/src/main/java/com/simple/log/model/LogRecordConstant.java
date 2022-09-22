package com.simple.log.model;

import java.util.regex.Pattern;

/**
 * @author fdrama
 * date 2022年09月22日 9:47
 */
public class LogRecordConstant {

    /**
     * 保存结果对象的变量的名称。
     */
    public static final String RESULT_VARIABLE = "_ret";

    /**
     * 保存结果的错误信息key
     */
    public static final String ERROR_MSG_VARIABLE = "_errorMsg";

    /**
     * 日志注解上的 pattern解析表达式
     */
    public static final Pattern LOG_PATTERN = Pattern.compile("\\{\\s*(\\w*)\\s*\\{(.*?)}}");

}
