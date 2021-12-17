package com.yu.dubbo.core.registry;

import com.yu.dubbo.core.handle.AppClientHandler;
import com.yu.dubbo.core.registry.domain.AppDeploy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.core.registry.domain.URL;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

import java.util.List;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_REMOVED;

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

    public void zkNotify(String service, TreeCacheEvent.Type operation, String address) {
        if (NODE_ADDED.equals(operation)) {
            if (null != getProviderFromRegistry(service)) {
                AppClientHandler.cacheAppServerAddress(service, getProviderFromRegistry(service).getAddressList());
            }
        } else if (NODE_REMOVED.equals(operation)) {
            AppClientHandler.removeAppServerAddress(service, address);
        }
    }
}
