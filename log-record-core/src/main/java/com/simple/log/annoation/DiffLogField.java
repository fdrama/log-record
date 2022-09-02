package com.simple.log.annoation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author fdrama
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DiffLogField {

    String name();

    String function() default "";
}
