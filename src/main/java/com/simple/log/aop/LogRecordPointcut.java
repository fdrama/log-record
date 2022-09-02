package com.simple.log.aop;

import com.simple.log.annoation.LogRecord;
import com.simple.log.parser.LogRecordResource;

import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * @author fdrama
 */
public class LogRecordPointcut extends StaticMethodMatcherPointcut implements Serializable {

    private static final long serialVersionUID = -7088363998197690618L;

    @Override
    public boolean matches(Method method, Class<?> targetClass) {

        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }

        Collection<LogRecord> annotations = LogRecordResource.getAnnotations(method, targetClass);
        return !annotations.isEmpty();
    }



}
