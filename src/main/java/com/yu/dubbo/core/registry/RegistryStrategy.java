package com.yu.dubbo.core.registry;

import com.yu.dubbo.core.registry.domain.AppDeploy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.core.registry.domain.URL;
import com.yu.dubbo.core.registry.zookeeper.ZookeeperRegistry;
import com.yu.dubbo.core.SpringContextHolder;

import java.util.List;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:09
 */
public class RegistryStrategy {
    private static RegistryFactory registryFactory;


    static {
        String registry = SpringContextHolder.getProperties("spring.registry", "zookeeper", String.class);
        if ("zookeeper".equals(registry)) {
            registryFactory = new ZookeeperRegistry();
        } else {
            throw new RuntimeException("[RegistryFactory] not support registry " + registry);
        }
    }

    public static void registerProvider(String service, String address) {
        registryFactory.registerProvider(new URL(service, address));
    }

    public static void registerConsumer(String service, String address) {
        registryFactory.registerConsumer(new URL(service, address));
    }

    public static void registerAppDeploy(String appName, AppDeploy appDeploy) {
        registryFactory.registerAppDeploy(appName, appDeploy);
    }

    public static AppServiceDomain.Provider getProviderFromRegistry(String serviceName) {
        return registryFactory.getProviderFromRegistry(serviceName);
    }

    public static List<AppDeploy> getAppDeployInfo() {
        return registryFactory.getAppDeployInfo();
    }

    public static void destroy() {
        registryFactory.destroy();
    }
}

