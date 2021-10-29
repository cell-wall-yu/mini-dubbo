package com.yu.dubbo.exception;

import org.slf4j.helpers.MessageFormatter;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:02
 */
public class DubboException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object errorData;

    public DubboException() {
        super();
    }

    public DubboException(String errorMsg) {
        super(errorMsg);
    }

    public DubboException(String format, String... argArray) {
        super(MessageFormatter.arrayFormat(format, argArray).getMessage());
    }

    public DubboException(Object errorMsg) {
        super(errorMsg != null ? errorMsg.toString() : "");
    }

    public DubboException(String errorMsg, Map<String, Object> errorMap) {
        super(errorMsg);
        this.errorData = errorMap;
    }

    public DubboException(RuntimeException e) {
        super(e);
    }

    public DubboException(Throwable e) {
        super(e);
    }

    public Object getErrorData() {
        return errorData;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message != null) {
            message = message.replace("com.yu.exception.DubboException", "");
        }
        return message;
    }
}