package com.simple.log.aop;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.simple.log.function.record.ILogRecordService;
import com.simple.log.function.user.IOperatorGetService;
import com.simple.log.model.CodeVariableType;
import com.simple.log.model.LogRecordDO;
import com.simple.log.model.LogRecordOps;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 */
@Slf4j
public class LogRecordInterceptor implements MethodInterceptor, Serializable, SmartInitializingSingleton {

    private static final long serialVersionUID = 31531527095537795L;

    private static final Pattern PATTERN = Pattern.compile("\\{\\s*(\\w*)\\s*\\{(.*?)}}");

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
        MethodExecuteResult methodExecuteResult = new MethodExecuteResult(true, null, "");

        // 需要在方法执行前调用的方法返回值
        Map<String, String> functionNameAndReturnMap = null;
        Collection<LogRecordOps> logRecordOps = null;
        try {
            // 获取注解信息
            logRecordOps = LogRecordResource.parseLogRecordAnnotations(method, targetClass);
            List<String> beforeExecuteFunctionTemplate = LogRecordResource.getBeforeExecuteFunctionTemplate(logRecordOps);
            functionNameAndReturnMap = processBeforeExecuteFunctionTemplate(beforeExecuteFunctionTemplate, targetClass, method, args);
        } catch (Exception e) {
            log.error("log record parse before function exception", e);
        }

        try {
            // 方法执行
            ret = invoker.proceed();
        } catch (Throwable e) {
            methodExecuteResult = new MethodExecuteResult(false, e, e.getMessage());
        }

        try {
            if (CollectionUtils.isNotEmpty(logRecordOps)) {
                recordExecute(ret, method, args, logRecordOps, targetClass,
                        methodExecuteResult.isSuccess(), methodExecuteResult.getErrorMsg(), functionNameAndReturnMap);
            }
        } catch (Exception t) {
            //记录日志错误不要影响业务
            log.error("log record parse exception", t);
        } finally {
            // 方法执行后清理日志上下文
            LogRecordContext.clear();
        }
        if (methodExecuteResult.throwable != null) {
            throw methodExecuteResult.throwable;
        }
        return ret;
    }

    private void recordExecute(Object ret, Method method, Object[] args, Collection<LogRecordOps> operations, Class<?> targetClass, boolean success, String errorMsg, Map<String, String> functionNameAndReturnMap) {

        for (LogRecordOps operation : operations) {
            try {
                // 获取日志成功模板 / 失败模板
                String content = getActionContent(success, operation);
                if (StringUtils.isEmpty(content)) {
                    //没有日志内容则忽略
                    continue;
                }
                //获取需要解析的表达式
                List<String> spElTemplates = LogRecordResource.getSpElTemplates(operation, content);

                String operatorIdFromService = getOperatorIdFromServiceAndPutTemplate(operation, spElTemplates);

                Map<String, String> expressionValues = processTemplate(spElTemplates, ret, targetClass, method, args, errorMsg, functionNameAndReturnMap);

                Map<CodeVariableType, String> codeVariable = getCodeVariable(method);

                if (logConditionPassed(operation.getCondition(), expressionValues)) {
                    LogRecordDO logRecord = LogRecordDO.builder()
                            .tenant(this.tenant)
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

                    //如果 action 为空，不记录日志
                    if (StringUtils.isEmpty(logRecord.getContent())) {
                        continue;
                    }
                    // 调用日志记录服务记录日志
                    Preconditions.checkNotNull(logRecordService, "logRecordService not init!!");
                    logRecordService.record(logRecord);
                }
            } catch (Exception t) {
                log.error("log record execute exception", t);
            }
        }
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

    private boolean logConditionPassed(String condition, Map<String, String> expressionValues) {
        return StringUtils.isEmpty(condition) || StringUtils.endsWithIgnoreCase(expressionValues.get(condition), "true");
    }

    private String getRealOperatorId(LogRecordOps operation, String operatorIdFromService, Map<String, String> expressionValues) {
        return StringUtils.isNotEmpty(operatorIdFromService) ? operatorIdFromService : expressionValues.get(operation.getOperatorId());
    }

    private String getActionContent(boolean success, LogRecordOps operation) {
        String action;
        if (success) {
            action = operation.getSuccessLogTemplate();
        } else {
            action = operation.getFailLogTemplate();
        }
        return action;
    }

    private Map<String, String> processBeforeExecuteFunctionTemplate(Collection<String> templates, Class<?> targetClass, Method method, Object[] args) {

        Map<String, String> functionNameAndReturnValueMap = new HashMap<>(templates.size());

        // 创建表达式解析上下文
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(method, args, targetClass, null, null, beanFactory);

        for (String expressionTemplate : templates) {
            if (expressionTemplate.contains("{")) {
                Matcher matcher = PATTERN.matcher(expressionTemplate);
                while (matcher.find()) {
                    String expressionStr = matcher.group(2);
                    if (expressionStr.contains("#_ret") || expressionStr.contains("#_errorMsg")) {
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


    public Map<String, String> processTemplate(Collection<String> templates, Object ret,
                                               Class<?> targetClass, Method method, Object[] args, String errorMsg,
                                               Map<String, String> beforeFunctionNameAndReturnMap) {

        // 解析结果
        Map<String, String> expressionValues = new HashMap<>(templates.size());

        // 创建表达式解析上下文 这里获取的入参如果发生了变更，那么上下文里的入参变量也是修改后的值
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(method, args, targetClass, ret, errorMsg, beanFactory);
        AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(method, targetClass);
        for (String expressionTemplate : templates) {
            // 利用正则表达式 matcher appendReplacement quoteReplacement appendTail 替换变量
            if (expressionTemplate.contains("{")) {
                Matcher matcher = PATTERN.matcher(expressionTemplate);
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class MethodExecuteResult {
        private boolean success;
        private Throwable throwable;
        private String errorMsg;
    }
}
