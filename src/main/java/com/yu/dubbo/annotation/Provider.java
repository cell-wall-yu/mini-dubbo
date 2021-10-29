package com.yu.dubbo.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author ycz
 * @title: Provider
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:03
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Provider {
    String value() default "";
}
