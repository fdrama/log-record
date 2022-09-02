package com.simple.log.config;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.lang.Nullable;

/**
 * @author fdrama
 */
public class LogRecordConfigurationSelector extends AdviceModeImportSelector<EnableLogRecord> {

    @Override
    @Nullable
    public String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY:
                return new String[] {LogRecordProxyAutoConfiguration.class.getName()};
            case ASPECTJ:
                return new String[] {LogRecordProxyAutoConfiguration.class.toString()};
            default:
                return null;
        }
    }
}
