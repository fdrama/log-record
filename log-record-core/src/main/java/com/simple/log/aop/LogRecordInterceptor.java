package com.simple.log.aop;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.simple.log.function.record.ILogRecordService;
import com.simple.log.function.user.IOperatorGetService;
import com.simple.log.model.CodeVariableType;
import com.simple.log.model.LogRecordConstant;
import com.simple.log.model.LogRecordDO;
import com.simple.log.model.LogRecordOps;
import com.simple.log.model.MethodExecuteResult;
import com.simple.log.parser.LogRecordContext;
import com.simple.log.parser.LogRecordExpressionEvaluator;
import com.simple.log.parser.LogRecordParser;
import com.simple.log.parser.LogRecordResource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import lombok.extern.slf4j.Slf4j;

import static com.simple.log.model.LogRecordConstant.LOG_PATTERN;

/**
 * @author fdrama
 */
@Slf4j
public class LogRecordInterceptor implements MethodInterceptor, Serializable, SmartInitializingSingleton {

    private static final long serialVersionUID = 31531527095537795L;

    private LogRecordParser logRecordParser;

    private ILogRecordService logRecordService;

    private IOperatorGetService operatorGetService;

    private BeanFactory beanFactory;

    private String tenant;

    protected final LogRecordExpressionEvaluator expressionEvaluator = new LogRecordExpressionEvaluator();


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        return execute(invocation, invocation.getThis(), method, invocation.getArguments());
    }

    private Object execute(MethodInvocation invoker, Object target, Method method, Object[] args) throws Throwable {

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        Object ret = null;
        // 方法执行前 初始化 日志上下文
        LogRecordContext.putEmptySpan();
        // 方法执行结果
        MethodExecuteResult methodExecuteResult = new MethodExecuteResult(method, args, targetClass);
        // 需要在方法执行前调用的方法返回值
        Map<String, String> functionNameAndReturnMap = null;
        Collection<LogRecordOps> logRecordOps = null;
        try {
            // 获取注解信息
            logRecordOps = LogRecordResource.parseLogRecordAnnotations(method, targetClass);
            List<String> beforeFunctionExecuteTemplate = LogRecordResource.getBeforeExecuteFunctionTemplate(logRecordOps);
            functionNameAndReturnMap = processBeforeExecuteFunctionTemplate(beforeFunctionExecuteTemplate, targetClass, method, args);
        } catch (Exception e) {
            log.error("log record parse before function exception", e);
        }

        try {
            // 方法执行
            ret = invoker.proceed();
            methodExecuteResult.setResult(ret);
            methodExecuteResult.setSuccess(true);
        } catch (Throwable e) {
            methodExecuteResult.setSuccess(false);
            methodExecuteResult.setErrorMsg(e.getMessage());
            methodExecuteResult.setThrowable(e);
        }


        try {
            if (CollectionUtils.isNotEmpty(logRecordOps)) {
                recordExecute(logRecordOps, methodExecuteResult, functionNameAndReturnMap);
            }
        } catch (Exception t) {
            //记录日志错误不要影响业务
            log.error("log record parse exception", t);
        } finally {
            // 方法执行后清理日志上下文
            LogRecordContext.clear();
        }
        if (methodExecuteResult.getThrowable() != null) {
            throw methodExecuteResult.getThrowable();
        }
        return ret;
    }

    private void recordExecute(Collection<LogRecordOps> operations, MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap) {

        for (LogRecordOps operation : operations) {
            try {
                // 获取日志成功模板 / 失败模板
                if (StringUtils.isEmpty(operation.getSuccessLogTemplate())
                        && StringUtils.isEmpty(operation.getFailLogTemplate())) {
                    continue;
                }

                // 不满足条件不记录日志
                if (!logConditionPassed(operation.getCondition(), methodExecuteResult, functionNameAndReturnMap)) {
                    continue;
                }

                if (methodExecuteResult.isSuccess()) {
                    successRecordExecute(operation, methodExecuteResult, functionNameAndReturnMap);
                } else {
                    failRecordExecute(operation, methodExecuteResult, functionNameAndReturnMap);
                }
            } catch (Exception t) {
                log.error("log record execute exception", t);
            }
        }
    }

    private void failRecordExecute(LogRecordOps operation, MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap) {
        if (StringUtils.isEmpty(operation.getFailLogTemplate())) {
            return;
        }
        //获取需要解析的表达式
        List<String> spElTemplates = LogRecordResource.getSpElTemplates(operation, operation.getFailLogTemplate());

        // 获取默认的操作人
        String operatorIdFromService = getOperatorIdFromServiceAndPutTemplate(operation, spElTemplates);
        // SPEl表达式解析后的值
        Map<String, String> expressionValues = processTemplate(spElTemplates, methodExecuteResult, functionNameAndReturnMap);

        saveLog(operation, methodExecuteResult, operation.getFailLogTemplate(), true, operatorIdFromService, expressionValues);
    }

    private void successRecordExecute(LogRecordOps operation, MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap) {

        String content;
        boolean success = true;
        // 存在保存成功/失败条件模板
        if (StringUtils.isNotEmpty(operation.getSuccessCondition())) {
            String successConditionValue = processSingleTemplate(operation.getSuccessCondition(), methodExecuteResult, functionNameAndReturnMap);
            if (StringUtils.endsWithIgnoreCase(successConditionValue, Boolean.toString(true))) {
                content = operation.getSuccessLogTemplate();
            } else {
                content = operation.getFailLogTemplate();
                success = false;

            }
        } else {
            content = operation.getSuccessLogTemplate();
        }
        if (StringUtils.isEmpty(content)) {
            // 没有日志内容忽略
            return;
        }
        //获取需要解析的表达式
        List<String> spElTemplates = LogRecordResource.getSpElTemplates(operation, content);
        // 获取默认的操作人
        String operatorIdFromService = getOperatorIdFromServiceAndPutTemplate(operation, spElTemplates);
        // SPEl表达式解析后的值
        Map<String, String> expressionValues = processTemplate(spElTemplates, methodExecuteResult, functionNameAndReturnMap);

        saveLog(operation, methodExecuteResult, content, success, operatorIdFromService, expressionValues);

    }

    private void saveLog(LogRecordOps operation, MethodExecuteResult methodExecuteResult, String content, boolean success, String operatorIdFromService, Map<String, String> expressionValues) {
        Map<CodeVariableType, String> codeVariable = getCodeVariable(methodExecuteResult.getMethod());

        LogRecordDO logRecord = LogRecordDO.builder()
                .tenant(tenant)
                .operator(getRealOperatorId(operation, operatorIdFromService, expressionValues))
                .bizNo(expressionValues.get(operation.getBizNo()))
                .type(expressionValues.get(operation.getType()))
                .subType(expressionValues.get(operation.getSubType()))
                .extra(expressionValues.get(operation.getExtra()))
                .className(codeVariable.get(CodeVariableType.ClassName))
                .methodName(codeVariable.get(CodeVariableType.MethodName))
                .content(expressionValues.get(content))
                .fail(!success)
                .createTime(System.currentTimeMillis())
                .build();

        // 调用日志记录服务记录日志
        Preconditions.checkNotNull(logRecordService, "logRecordService not init!!");
        logRecordService.record(logRecord);
    }

    private Map<CodeVariableType, String> getCodeVariable(Method method) {
        Map<CodeVariableType, String> map = Maps.newHashMap();
        map.put(CodeVariableType.ClassName, method.getDeclaringClass().toString());
        map.put(CodeVariableType.MethodName, method.getName());
        return map;
    }

    private String getOperatorIdFromServiceAndPutTemplate(LogRecordOps operation, List<String> spElTemplates) {

        String realOperatorId = "";
        if (StringUtils.isEmpty(operation.getOperatorId())) {
            realOperatorId = operatorGetService.getUser().getOperatorId();
            if (StringUtils.isEmpty(realOperatorId)) {
                throw new IllegalArgumentException("[LogRecord] operator is null");
            }
        } else {
            spElTemplates.add(operation.getOperatorId());
        }
        return realOperatorId;
    }

    private boolean logConditionPassed(String condition, MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap) {
        if (StringUtils.isEmpty(condition)) {
            return true;
        }
        String conditionValue = processSingleTemplate(condition, methodExecuteResult, functionNameAndReturnMap);
        return StringUtils.equalsIgnoreCase(Boolean.toString(true), conditionValue);
    }

    private String getRealOperatorId(LogRecordOps operation, String operatorIdFromService, Map<String, String> expressionValues) {
        return StringUtils.isNotEmpty(operatorIdFromService) ? operatorIdFromService : expressionValues.get(operation.getOperatorId());
    }

    private Map<String, String> processBeforeExecuteFunctionTemplate(Collection<String> templates, Class<?> targetClass, Method method, Object[] args) {

        Map<String, String> functionNameAndReturnValueMap = new HashMap<>(templates.size());

        // 创建表达式解析上下文
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(method, args, targetClass, null, null, beanFactory);

        for (String expressionTemplate : templates) {
            if (expressionTemplate.contains("{")) {
                Matcher matcher = LOG_PATTERN.matcher(expressionTemplate);
                while (matcher.find()) {
                    String expressionStr = matcher.group(2);
                    // 方法执行前 没有结果参数
                    if (expressionStr.contains(LogRecordConstant.RESULT_VARIABLE) || expressionStr.contains(LogRecordConstant.ERROR_MSG_VARIABLE)) {
                        continue;
                    }
                    String functionName = matcher.group(1);
                    // 需要在调用方法执行前执行的方法
                    if (StringUtils.isNotEmpty(functionName) && logRecordParser.beforeFunction(functionName)) {
                        AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(method, targetClass);
                        // 调用方法获取方法返回值
                        String functionReturnValue = logRecordParser.getFunctionReturnValue(null, expressionStr, functionName, evaluationContext, annotatedElementKey);
                        // 缓存方法返回值
                        String functionCallInstanceKey = logRecordParser.getFunctionCallInstanceKey(functionName, expressionStr);
                        functionNameAndReturnValueMap.put(functionCallInstanceKey, functionReturnValue);
                    }
                }
            }
        }
        return functionNameAndReturnValueMap;
    }

    public String processSingleTemplate(String templates, MethodExecuteResult methodExecuteResult,
                                        Map<String, String> functionNameAndReturnMap) {
        Map<String, String> stringStringMap = processTemplate(Collections.singletonList(templates), methodExecuteResult,
                functionNameAndReturnMap);
        return stringStringMap.get(templates);
    }

    public Map<String, String> processTemplate(Collection<String> templates, MethodExecuteResult methodExecuteResult,
                                               Map<String, String> beforeFunctionNameAndReturnMap) {

        // 解析结果
        Map<String, String> expressionValues = new HashMap<>(templates.size());

        // 创建表达式解析上下文 这里获取的入参如果发生了变更，那么上下文里的入参变量也是修改后的值
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(methodExecuteResult.getMethod(), methodExecuteResult.getArgs(),
                methodExecuteResult.getTargetClass(), methodExecuteResult.getResult(),
                methodExecuteResult.getErrorMsg(), beanFactory);
        AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(methodExecuteResult.getMethod(), methodExecuteResult.getTargetClass());
        for (String expressionTemplate : templates) {
            // 利用正则表达式 matcher appendReplacement quoteReplacement appendTail 替换变量
            if (expressionTemplate.contains("{")) {
                Matcher matcher = LOG_PATTERN.matcher(expressionTemplate);
                StringBuffer parsedStr = new StringBuffer();
                while (matcher.find()) {
                    String functionName = matcher.group(1);
                    String expressionStr = matcher.group(2);
                    String expressionValue;
                    if (StringUtils.isEmpty(functionName)) {
                        Object value = expressionEvaluator.parseExpression(expressionStr, annotatedElementKey, evaluationContext);
                        expressionValue = Objects.nonNull(value) ? value.toString() : StringUtils.EMPTY;
                    } else {
                        expressionValue = logRecordParser.getFunctionReturnValue(beforeFunctionNameAndReturnMap, expressionStr, functionName, evaluationContext, annotatedElementKey);
                    }
                    matcher.appendReplacement(parsedStr, Matcher.quoteReplacement(Strings.nullToEmpty(expressionValue)));
                }
                matcher.appendTail(parsedStr);
                expressionValues.put(expressionTemplate, parsedStr.toString());
            } else {
                expressionValues.put(expressionTemplate, expressionTemplate);
            }
        }
        return expressionValues;
    }

    @Override
    public void afterSingletonsInstantiated() {
        setLogRecordService(beanFactory.getBean(ILogRecordService.class));
        setOperatorGetService(beanFactory.getBean(IOperatorGetService.class));
        setLogFunctionParser(beanFactory.getBean(LogRecordParser.class));
    }

    @Autowired
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setLogRecordService(ILogRecordService logRecordService) {
        this.logRecordService = logRecordService;
    }

    public void setOperatorGetService(IOperatorGetService operatorGetService) {
        this.operatorGetService = operatorGetService;
    }

    public void setLogFunctionParser(LogRecordParser logRecordParser) {
        this.logRecordParser = logRecordParser;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

}
