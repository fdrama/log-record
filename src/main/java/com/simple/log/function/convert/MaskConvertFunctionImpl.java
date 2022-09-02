package com.simple.log.function.convert;

import com.simple.log.utils.SensitiveUtils;

/**
 * @author fdrama
 * date 2022年09月02日 15:00
 */
public class MaskConvertFunctionImpl implements IConvertFunction {

    @Override
    public String functionName() {
        return "_MASK";
    }

    @Override
    public String convert(Object... values) {
        return SensitiveUtils.replace(values);
    }
}
