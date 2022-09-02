package com.simple.log.utils;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author fdrama
 */
public class LocalDateTimeUtils {

    public static final String SIMPLE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String getLocalDateStr(String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            return getLocalDateStr(SIMPLE_PATTERN);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(LocalDateTime.now());
    }
}
