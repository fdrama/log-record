package com.simple.log.parser;

import com.simple.log.function.IParseFunctionService;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author muzhantong
 * create on 2022/1/5 8:37 下午
 */
public class LogRecordParser implements BeanFactoryAware, SmartInitializingSingleton {

    protected BeanFactory beanFactory;
    protected final LogRecordExpressionEvaluator expressionEvaluator = new LogRecordExpressionEvaluator();

    private IParseFunctionService functionService;

    public static final String COMMA = ",";

    /**
     * 方法参数支持多个
     *
     * @param beforeFunctionNameAndReturnMap 缓存的方法和返回值
     * @param expressionStr                  方法参数表达式 ,分割
     * @param functionName                   调用方法名称
     * @return
     */
    public String getFunctionReturnValue(Map<String, String> beforeFunctionNameAndReturnMap, String expressionStr, String functionName,
                                         EvaluationContext evaluationContext, AnnotatedElementKey annotatedElementKey) {

        // 1. 拆分方法参数值
        String[] expressionArray;
        if (expressionStr.contains(COMMA)) {
            expressionArray = expressionStr.split(COMMA);
        } else {
            expressionArray = new String[]{expressionStr};
        }

        List<Object> args = new ArrayList<>(expressionArray.length);
        // 2. 解析方法参数spEL
        for (String expression : expressionArray) {
            Object arg = expressionEvaluator.parseExpression(expression, annotatedElementKey, evaluationContext);
            args.add(arg);
        }
        // 3.调用function获取返回值
        String functionReturnValue;
        String functionCallInstanceKey = getFunctionCallInstanceKey(functionName, expressionStr);
        if (beforeFunctionNameAndReturnMap != null && beforeFunctionNameAndReturnMap.containsKey(functionCallInstanceKey)) {
            functionReturnValue = beforeFunctionNameAndReturnMap.get(functionCallInstanceKey);
        } else {
            functionReturnValue = functionService.apply(functionName, args.toArray());
        }
        return functionReturnValue;
    }

    /**
     * @param functionName    函数名称
     * @param paramExpression 解析前的表达式
     * @return 函数缓存的key
     * 方法执行之前换成函数的结果，此时函数调用的唯一标志：函数名+参数表达式
     */
    public String getFunctionCallInstanceKey(String functionName, String paramExpression) {
        return functionName + paramExpression;
    }


    public boolean beforeFunction(String functionName) {
        return functionService.beforeFunction(functionName);
    }


    public void setFunctionService(IParseFunctionService functionService) {
        this.functionService = functionService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        setFunctionService(beanFactory.getBean(IParseFunctionService.class));
    }
}
