package com.simple.log.parser;

import com.google.common.collect.Lists;

import com.simple.log.annoation.LogRecord;
import com.simple.log.model.LogRecordOps;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author fdrama
 */
public class LogRecordResource {


    public static Collection<LogRecord> getAnnotations(Method method, Class<?> targetClass) {
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        return AnnotatedElementUtils.findAllMergedAnnotations(specificMethod, LogRecord.class);
    }

    public static Collection<LogRecordOps> parseLogRecordAnnotations(Method method, Class<?> targetClass) {

        Collection<LogRecord> logRecordAnnotationAnnotations = getAnnotations(method, targetClass);

        if (logRecordAnnotationAnnotations.isEmpty()) {
            return new ArrayList<>();
        }
        Collection<LogRecordOps> ret = new ArrayList<>();
        for (LogRecord recordAnnotation : logRecordAnnotationAnnotations) {
            ret.add(parseLogRecordAnnotation(recordAnnotation));
        }
        return ret;
    }

    private static LogRecordOps parseLogRecordAnnotation(LogRecord recordAnnotation) {
        return LogRecordOps.builder()
                .successLogTemplate(recordAnnotation.success())
                .failLogTemplate(recordAnnotation.fail())
                .type(recordAnnotation.type())
                .subType(recordAnnotation.subType())
                .bizNo(recordAnnotation.businessId())
                .operatorId(recordAnnotation.operator())
                .extra(recordAnnotation.extra())
                .condition(recordAnnotation.condition())
                .build();
    }

    public static List<String> getBeforeExecuteFunctionTemplate(Collection<LogRecordOps> operations) {
        List<String> spElTemplates = new ArrayList<>();
        for (LogRecordOps operation : operations) {
            //执行之前的函数，失败模版不解析
            List<String> templates = getSpElTemplates(operation, operation.getSuccessLogTemplate());
            if (!CollectionUtils.isEmpty(templates)) {
                spElTemplates.addAll(templates);
            }
        }
        return spElTemplates;
    }

    public static List<String> getSpElTemplates(LogRecordOps operation, String action) {
        List<String> spElTemplates = Lists.newArrayList(operation.getType(), operation.getBizNo(), operation.getSubType(), action, operation.getExtra());
        if (!StringUtils.hasText(operation.getCondition())) {
            spElTemplates.add(operation.getCondition());
        }
        return spElTemplates;
    }


}
