package com.simple.log.function;

import com.simple.log.function.convert.IConvertFunction;

import org.apache.commons.lang3.StringUtils;

/**
 * @author fdrama
 * date 2022年09月02日 16:24
 */
public class DefaultConvertFunctionServiceImpl implements IConvertFunctionService{

    private final ConvertFunctionFactory convertFunctionFactory;

    public DefaultConvertFunctionServiceImpl(ConvertFunctionFactory convertFunctionFactory) {
        this.convertFunctionFactory = convertFunctionFactory;
    }

    public String convert(String functionName, Object... values) {
        IConvertFunction function = convertFunctionFactory.getFunction(functionName);
        if (function == null) {
            return StringUtils.EMPTY;
        }
        return function.convert(values);
    }


    @Override
    public boolean beforeFunction(String functionName) {
        return false;
    }
}
