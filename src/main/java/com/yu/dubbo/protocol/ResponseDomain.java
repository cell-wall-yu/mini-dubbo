package com.yu.dubbo.protocol;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:25
 */
public class ResponseDomain {
    /**
     * 状态码
     */
    private Integer code = 0;

    /**
     * 业务执行耗费时间
     */
    private Integer costTime = -1;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 返回的数据对象
     */
    private Object result;

    public ResponseDomain() {
    }

    public ResponseDomain(Integer code, String message, Object result) {
        this.code = code;
        this.result = result;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Integer getCostTime() {
        return costTime;
    }

    public void setCostTime(Integer costTime) {
        this.costTime = costTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
