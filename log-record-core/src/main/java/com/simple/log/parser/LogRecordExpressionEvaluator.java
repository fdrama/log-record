package com.simple.log.parser;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fdrama
 */
public class LogRecordExpressionEvaluator extends CachedExpressionEvaluator {

    private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>(64);
    private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>(64);

    /**
     * 解析指定表达式。
     */
    public Object parseExpression(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.expressionCache, methodKey, conditionExpression).getValue(evalContext, Object.class);
    }


    /**
     * Create an {@link EvaluationContext}.
     *
     * @param method      the method
     * @param args        the method arguments
     * @param targetClass the target class
     * @param result      the return value (can be {@code null}) or
     * @param errorMsg    errorMsg
     * @param beanFactory Spring beanFactory
     * @return the evaluation context
     */
    public EvaluationContext createEvaluationContext(Method method, Object[] args, Class<?> targetClass,
                                                     Object result, String errorMsg, BeanFactory beanFactory) {
        Method targetMethod = getTargetMethod(targetClass, method);
        LogRecordEvaluationContext evaluationContext = new LogRecordEvaluationContext(
                null, targetMethod, args, getParameterNameDiscoverer());

        if (beanFactory != null) {
            evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }

        evaluationContext.addResult(result);
        evaluationContext.addErrorMsg(errorMsg);
        evaluationContext.setVariables(LogRecordContext.getVariables());

        return evaluationContext;
    }

    /**
     * 获取真正的具体方法
     *
     * @param targetClass 目标class
     * @param method      来自接口或者父类的方法签名
     * @return 目标class实现的具体方法
     */
    private Method getTargetMethod(Class<?> targetClass, Method method) {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
        Method targetMethod = this.targetMethodCache.get(methodKey);
        if (targetMethod == null) {
            //能从代理对象上的一个方法，找到真实对象上对应的方法
            targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            this.targetMethodCache.put(methodKey, targetMethod);
        }
        return targetMethod;
    }
}
