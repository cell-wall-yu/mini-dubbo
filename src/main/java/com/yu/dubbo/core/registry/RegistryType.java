package com.yu.dubbo.core.registry;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:05
 */
public enum RegistryType {

    AppService("/mini-dubbo/app_service"),

    GateClient("/mini-dubbo/gate_client"),

    AppDeploy("/mini-dubbo/app_deploy");

    private String path;

    private RegistryType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}

