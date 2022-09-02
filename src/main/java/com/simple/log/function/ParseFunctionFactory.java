package com.simple.log.function;

import com.simple.log.function.parse.IParseFunction;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fdrama
 */
public class ParseFunctionFactory {

    private Map<String, IParseFunction> parseFunctionMap;
    public ParseFunctionFactory(List<IParseFunction> parseFunctions) {
        if (CollectionUtils.isEmpty(parseFunctions)) {
            return;
        }
        parseFunctionMap = new HashMap<>();
        for (IParseFunction parseFunction : parseFunctions) {
            if (StringUtils.isEmpty(parseFunction.functionName())) {
                continue;
            }
            parseFunctionMap.put(parseFunction.functionName(), parseFunction);
        }
    }

    public IParseFunction getFunction(String functionName) {
        return parseFunctionMap.get(functionName);
    }

    public boolean isBeforeFunction(String functionName) {
        return parseFunctionMap.get(functionName) != null && parseFunctionMap.get(functionName).executeBefore();
    }
}
