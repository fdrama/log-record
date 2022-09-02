package com.simple.log.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AbstractLogRecordConfiguration implements ImportAware {

    @Nullable
    protected AnnotationAttributes enableLogRecord;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {

        this.enableLogRecord = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableLogRecord.class.getName(), false));
        if (this.enableLogRecord == null) {
            log.info("EnableLogRecord is not present on importing class");
        }
    }
}
