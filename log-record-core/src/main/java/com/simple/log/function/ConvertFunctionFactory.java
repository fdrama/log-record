package com.simple.log.function;

import com.simple.log.function.convert.IConvertFunction;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fdrama
 */
public class ConvertFunctionFactory {

    private Map<String, IConvertFunction> convertFunctionMap;

    public ConvertFunctionFactory(List<IConvertFunction> convertFunctions) {
        if (CollectionUtils.isEmpty(convertFunctions)) {
            return;
        }
        convertFunctionMap = new HashMap<>();
        for (IConvertFunction convertFunction : convertFunctions) {
            if (StringUtils.isEmpty(convertFunction.functionName())) {
                continue;
            }
            convertFunctionMap.put(convertFunction.functionName(), convertFunction);
        }
    }

    public IConvertFunction getFunction(String functionName) {
        return convertFunctionMap.get(functionName);
    }

}
