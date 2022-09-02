package com.simple.log.config;


import com.simple.log.aop.LogRecordInterceptor;
import com.simple.log.aop.LogRecordPointAdvisor;
import com.simple.log.function.ConvertFunctionFactory;
import com.simple.log.function.DefaultConvertFunctionServiceImpl;
import com.simple.log.function.DefaultParseFunctionServiceImpl;
import com.simple.log.function.IConvertFunctionService;
import com.simple.log.function.IParseFunctionService;
import com.simple.log.function.ParseFunctionFactory;
import com.simple.log.function.convert.IConvertFunction;
import com.simple.log.function.convert.MaskConvertFunctionImpl;
import com.simple.log.function.diff.DefaultDiffItemsToLogContentServiceImpl;
import com.simple.log.function.diff.IDiffItemsToLogContentService;
import com.simple.log.function.diff.LogRecordProperties;
import com.simple.log.function.diff.ObjectDifferUtils;
import com.simple.log.function.parse.DiffParseFunctionImpl;
import com.simple.log.function.parse.IParseFunction;
import com.simple.log.function.parse.NowParseFunctionImpl;
import com.simple.log.function.record.DefaultLogRecordServiceImpl;
import com.simple.log.function.record.ILogRecordService;
import com.simple.log.function.user.DefaultOperatorGetServiceImpl;
import com.simple.log.function.user.IOperatorGetService;
import com.simple.log.parser.LogRecordParser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 */
@Configuration()
@EnableConfigurationProperties({LogRecordProperties.class})
@Slf4j
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class LogRecordProxyAutoConfiguration extends AbstractLogRecordConfiguration {


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogRecordPointAdvisor logRecordAdvisor() {
        LogRecordPointAdvisor advisor =
                new LogRecordPointAdvisor();
        advisor.setAdvice(logRecordInterceptor());
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogRecordInterceptor logRecordInterceptor() {
        LogRecordInterceptor interceptor = new LogRecordInterceptor();
        assert enableLogRecord != null;
        interceptor.setTenant(enableLogRecord.getString("tenant"));
        return interceptor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogRecordParser logRecordParser() {
        return new LogRecordParser();
    }

    @Bean
    public ParseFunctionFactory parseFunctionFactory(@Autowired List<IParseFunction> parseFunctions) {
        return new ParseFunctionFactory(parseFunctions);
    }

    @Bean
    public ConvertFunctionFactory convertFunctionFactory(@Autowired List<IConvertFunction> convertFunctions) {
        return new ConvertFunctionFactory(convertFunctions);
    }


    @Bean
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IParseFunctionService parseFunctionService(ParseFunctionFactory parseFunctionFactory) {
        return new DefaultParseFunctionServiceImpl(parseFunctionFactory);
    }

    @Bean
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IConvertFunctionService convertFunctionService(ConvertFunctionFactory convertFunctionFactory) {
        return new DefaultConvertFunctionServiceImpl(convertFunctionFactory);
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public IParseFunction nowParseFunction() {
        return new NowParseFunctionImpl();
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public IParseFunction diffParseFunction(IDiffItemsToLogContentService diffItemsToLogContentService) {
        DiffParseFunctionImpl diffParseFunction = new DiffParseFunctionImpl();
        diffParseFunction.setObjectDifferUtils(objectDifferUtils(diffItemsToLogContentService));
        return diffParseFunction;
    }

    @Bean
    public ObjectDifferUtils objectDifferUtils(IDiffItemsToLogContentService diffItemsToLogContentService) {
        ObjectDifferUtils diffParseFunction = new ObjectDifferUtils();
        diffParseFunction.setDiffItemsToLogContentService(diffItemsToLogContentService);
        return diffParseFunction;
    }

    @Bean
    @ConditionalOnMissingBean(IDiffItemsToLogContentService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IDiffItemsToLogContentService diffItemsToLogContentService(LogRecordProperties logRecordProperties, IConvertFunctionService convertFunctionService) {
        return new DefaultDiffItemsToLogContentServiceImpl(logRecordProperties, convertFunctionService);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public IConvertFunction convertFunction() {
        return new MaskConvertFunctionImpl();
    }


    @Bean
    @ConditionalOnMissingBean(IOperatorGetService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IOperatorGetService operatorGetService() {
        return new DefaultOperatorGetServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(ILogRecordService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public ILogRecordService recordService() {
        return new DefaultLogRecordServiceImpl();
    }


}
