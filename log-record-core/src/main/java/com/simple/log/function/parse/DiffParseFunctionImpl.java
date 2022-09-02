package com.simple.log.function.parse;

import com.simple.log.function.diff.ObjectDifferUtils;

/**
 * @author fdrama
 * @date 2022年09月01日 17:26
 */
public class DiffParseFunctionImpl implements IParseFunction {

    private ObjectDifferUtils objectDifferUtils;

    @Override
    public String functionName() {
        return DefaultParseFunctionEnum.DIFF.getFunctionName();
    }

    @Override
    public String apply(Object... values) {
        if (values.length == 2) {
            return objectDifferUtils.diff(values[0], values[1]);
        }
        return null;
    }

    public void setObjectDifferUtils(ObjectDifferUtils objectDifferUtils) {
        this.objectDifferUtils = objectDifferUtils;
    }
}
