package com.simple.log.aop;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * @author fdrama
 */
public class LogRecordPointAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    LogRecordPointcut pointcut = new LogRecordPointcut();

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public void setAdvice(Advice advice) {
        super.setAdvice(advice);
    }
}
