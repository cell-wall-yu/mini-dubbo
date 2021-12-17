package com.yu.dubbo.annotation;


import java.lang.annotation.*;

/**
 * 用户远程接口引用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Reference {
}
