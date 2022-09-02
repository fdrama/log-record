package com.simple.log.parser;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

/**
 * DATE 4:12 PM
 *
 * @author mzt.
 */
public class LogRecordEvaluationContext extends MethodBasedEvaluationContext {

    /**
     * 保存结果对象的变量的名称。
     */
    public static final String RESULT_VARIABLE = "_ret";

    public static final String ERROR_MSG_VARIABLE = "_errorMsg";

    /**
     * 把方法的参数都放到 SpEL 解析的 RootObject
     */
    public LogRecordEvaluationContext(Object rootObject, Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
        super(rootObject, method, arguments, parameterNameDiscoverer);
    }

    /**
     * 添加方法的返回值到 rootObject
     *
     * @param ret
     */
    public void addResult(Object ret) {
        setVariable(RESULT_VARIABLE, ret);
    }

    /**
     * 添加方法的错误信息到 rootObject
     *
     * @param errorMsg
     */
    public void addErrorMsg(String errorMsg) {
        setVariable(ERROR_MSG_VARIABLE, errorMsg);
    }
}