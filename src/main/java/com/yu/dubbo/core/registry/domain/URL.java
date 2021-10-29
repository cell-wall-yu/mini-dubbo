package com.yu.dubbo.core.registry.domain;

public class URL {
    private String className;
    private String address;

    public URL(String className, String address) {
        this.className = className;
        this.address = address;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
