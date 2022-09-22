package com.simple.log.regrex;

import com.simple.log.model.LogRecordConstant;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 * date 2022年09月02日 17:53
 */
@Slf4j
public class RegexTest {


    @Test
    public void testEmptyFunction() {
        String expressionTemplate = "{{#newOrder.orderNo}}";
        Matcher matcher = LogRecordConstant.LOG_PATTERN.matcher(expressionTemplate);
        while (matcher.find()) {
            String functionName = matcher.group(1);
            String expressionStr = matcher.group(2);
            System.out.println(functionName);
            System.out.println(expressionStr);

        }
    }

    @Test
    public void testFunction() {
        String expressionTemplate = "{simple{#newOrder.orderNo}}";
        Matcher matcher = LogRecordConstant.LOG_PATTERN.matcher(expressionTemplate);
        while (matcher.find()) {
            String functionName = matcher.group(1);
            String expressionStr = matcher.group(2);
            System.out.println(functionName);
            System.out.println(expressionStr);
        }
    }
}
