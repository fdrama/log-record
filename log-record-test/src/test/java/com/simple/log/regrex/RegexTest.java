package com.simple.log.regrex;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 * date 2022年09月02日 17:53
 */
@Slf4j
public class RegexTest {

    private static final Pattern PATTERN = Pattern.compile("\\{\\s*(\\w*)\\s*\\{(.*?)}}");

    @Test
    public void test1() {
        String expressionTemplate = "{{#newOrder.orderNo}}";
        Matcher matcher = PATTERN.matcher(expressionTemplate);
        while (matcher.find()) {
            String functionName = matcher.group(1);
            String expressionStr = matcher.group(2);
            System.out.println(functionName);
            System.out.println(expressionStr);

        }
    }
}
