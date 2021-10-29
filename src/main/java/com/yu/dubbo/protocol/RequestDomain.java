package com.yu.dubbo.protocol;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:25
 */
public class RequestDomain {
    /**
     * 请求号
     */
    private String requestNo;

    /**
     * 接口class name
     */
    private String className;

    /**
     * 目标方法名
     */
    private String methodName;

    /**
     * 入参类型
     */
    private String[] paramTypeNames;

    /**
     * 入参
     */
    private Object[] paramInputs;

    public RequestDomain() {
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getParamTypeNames() {
        return paramTypeNames;
    }

    public void setParamTypeNames(String[] paramTypeNames) {
        this.paramTypeNames = paramTypeNames;
    }

    public Object[] getParamInputs() {
        return paramInputs;
    }

    public void setParamInputs(Object[] paramInputs) {
        this.paramInputs = paramInputs;
    }
}
