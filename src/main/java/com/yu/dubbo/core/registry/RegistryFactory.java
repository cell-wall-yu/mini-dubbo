package com.yu.dubbo.core.registry;

import com.yu.dubbo.core.registry.domain.AppDeploy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.core.registry.domain.URL;

import java.util.List;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:08
 */
public abstract class RegistryFactory {


    public abstract void registerConsumer(URL url);

    public abstract void registerProvider(URL url);

    public abstract void registerAppDeploy(String appName, AppDeploy appDeploy);

    public abstract AppServiceDomain.Provider getProviderFromRegistry(String serviceName);

    public abstract List<AppDeploy> getAppDeployInfo();

    public abstract void destroy();
}
