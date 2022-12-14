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
        // ??????????????? ????????? ???????????????
        LogRecordContext.putEmptySpan();
        // ??????????????????
        MethodExecuteResult methodExecuteResult = new MethodExecuteResult(method, args, targetClass);
        // ????????????????????????????????????????????????
        Map<String, String> functionNameAndReturnMap = null;
        Collection<LogRecordOps> logRecordOps = null;
        try {
            // ??????????????????
            logRecordOps = LogRecordResource.parseLogRecordAnnotations(method, targetClass);
            List<String> beforeFunctionExecuteTemplate = LogRecordResource.getBeforeExecuteFunctionTemplate(logRecordOps);
            functionNameAndReturnMap = processBeforeExecuteFunctionTemplate(beforeFunctionExecuteTemplate, targetClass, method, args);
        } catch (Exception e) {
            log.error("log record parse before function exception", e);
        }

        try {
            // ????????????
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
            //????????????????????????????????????
            log.error("log record parse exception", t);
        } finally {
            // ????????????????????????????????????
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
                // ???????????????????????? / ????????????
                if (StringUtils.isEmpty(operation.getSuccessLogTemplate())
                        && StringUtils.isEmpty(operation.getFailLogTemplate())) {
                    continue;
                }

                // ??????????????????????????????
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
        //??????????????????????????????
        List<String> spElTemplates = LogRecordResource.getSpElTemplates(operation, operation.getFailLogTemplate());

        // ????????????????????????
        String operatorIdFromService = getOperatorIdFromServiceAndPutTemplate(operation, spElTemplates);
        // SPEl????????????????????????
        Map<String, String> expressionValues = processTemplate(spElTemplates, methodExecuteResult, functionNameAndReturnMap);

        saveLog(operation, methodExecuteResult, operation.getFailLogTemplate(), true, operatorIdFromService, expressionValues);
    }

    private void successRecordExecute(LogRecordOps operation, MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap) {

        String content;
        boolean success = true;
        // ??????????????????/??????????????????
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
            // ????????????????????????
            return;
        }
        //??????????????????????????????
        List<String> spElTemplates = LogRecordResource.getSpElTemplates(operation, content);
        // ????????????????????????
        String operatorIdFromService = getOperatorIdFromServiceAndPutTemplate(operation, spElTemplates);
        // SPEl????????????????????????
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

        // ????????????????????????????????????
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

        // ??????????????????????????????
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(method, args, targetClass,
                null, null, beanFactory);

        for (String expressionTemplate : templates) {
            if (expressionTemplate.contains("{")) {
                Matcher matcher = LOG_PATTERN.matcher(expressionTemplate);
                while (matcher.find()) {
                    String expressionStr = matcher.group(2);
                    // ??????????????? ??????????????????
                    if (expressionStr.contains(LogRecordConstant.RESULT_VARIABLE) || expressionStr.contains(LogRecordConstant.ERROR_MSG_VARIABLE)) {
                        continue;
                    }
                    String functionName = matcher.group(1);
                    // ?????????????????????????????????????????????
                    if (StringUtils.isNotEmpty(functionName) && logRecordParser.beforeFunction(functionName)) {
                        AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(method, targetClass);
                        // ?????????????????????????????????
                        String functionReturnValue = logRecordParser.getFunctionReturnValue(null, expressionStr, functionName, evaluationContext, annotatedElementKey);
                        // ?????????????????????
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

        // ????????????
        Map<String, String> expressionValues = new HashMap<>(templates.size());

        // ?????????????????????????????? ???????????????????????????????????????????????????????????????????????????????????????????????????
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(methodExecuteResult.getMethod(),
                methodExecuteResult.getArgs(),
                methodExecuteResult.getTargetClass(),
                methodExecuteResult.getResult(),
                methodExecuteResult.getErrorMsg(),
                beanFactory);
        AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(methodExecuteResult.getMethod(), methodExecuteResult.getTargetClass());
        for (String expressionTemplate : templates) {
            // ????????????????????? matcher appendReplacement quoteReplacement appendTail ????????????
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
