package com.simple.log.function;


import com.simple.log.function.parse.IParseFunction;

import org.apache.commons.lang3.StringUtils;

/**
 * @author muzhantong
 */
public class DefaultParseFunctionServiceImpl implements IParseFunctionService {

    private final ParseFunctionFactory parseFunctionFactory;

    public DefaultParseFunctionServiceImpl(ParseFunctionFactory parseFunctionFactory) {
        this.parseFunctionFactory = parseFunctionFactory;
    }

    @Override
    public String apply(String functionName, Object... values) {
        IParseFunction function = parseFunctionFactory.getFunction(functionName);
        if (function == null) {
            return StringUtils.EMPTY;
        }
        return function.apply(values);
    }

    @Override
    public boolean beforeFunction(String functionName) {
        return parseFunctionFactory.isBeforeFunction(functionName);
    }

}
