package com.simple.log.parser;

import com.simple.log.model.LogRecordConstant;

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
        setVariable(LogRecordConstant.RESULT_VARIABLE, ret);
    }

    /**
     * 添加方法的错误信息到 rootObject
     *
     * @param errorMsg
     */
    public void addErrorMsg(String errorMsg) {
        setVariable(LogRecordConstant.ERROR_MSG_VARIABLE, errorMsg);
    }
}