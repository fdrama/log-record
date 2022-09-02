package com.simple.log.function.parse;


import com.simple.log.utils.LocalDateTimeUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author muzhantong
 */
public class NowParseFunctionImpl implements IParseFunction {

    @Override
    public boolean executeBefore() {
        return true;
    }

    @Override
    public String functionName() {
        return DefaultParseFunctionEnum.NOW.getFunctionName();
    }

    @Override
    public String apply(Object... values) {
        Optional<Object> first = Arrays.stream(values).findFirst();
        if (!first.isPresent()) {
            return StringUtils.EMPTY;
        }
        return LocalDateTimeUtils.getLocalDateStr(String.valueOf(first.get()));
    }
}
