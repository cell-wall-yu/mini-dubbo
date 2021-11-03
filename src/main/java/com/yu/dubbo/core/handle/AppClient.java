package com.yu.dubbo.core.handle;

import com.yu.dubbo.core.registry.RegistryStrategy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.utils.CommonUtil;
import com.yu.dubbo.core.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:20
 */
public class AppClient {

    private static Logger log = LoggerFactory.getLogger(AppClient.class);

    // 线程池
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 创建直连代理实例
     *
     * @param interfaces
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T proxy(Class<T> interfaces) {
        if (StringUtils.isEmpty(SpringContextHolder.getProperties("spring.application.name")) || StringUtils.isEmpty(SpringContextHolder.getProperties("server.port"))) {
            throw new RuntimeException("请检查应用名{spring.application.name}和端口号{server.port}");
        }

        String localhostHttpAddress = CommonUtil.getLocalServerAddress() +
                "?appName=" + SpringContextHolder.getProperties("spring.application.name") + "&service=" +
                interfaces.getName();

        AppClientHandler.registerAppClientServiceName(interfaces.getName());

        // 注册消费者
        log.info("[app-client] registry customerAddress to registry-center: url: {},service: {}", localhostHttpAddress, interfaces.getName());
        RegistryStrategy.registerConsumer(interfaces.getName(), localhostHttpAddress);


        // 缓存type.getName()对应的服务地址
        AppServiceDomain.Provider provider = RegistryStrategy.getProviderFromRegistry(interfaces.getName());
        if (provider != null && provider.getAddressList() != null) {
            AppClientHandler.cacheAppServerAddress(provider.getServiceName(), provider.getAddressList());
        }

        return (T) Proxy.newProxyInstance(AppClient.class.getClassLoader(), new Class<?>[]{interfaces}, (proxy, method, args) -> {
            AppClientHandler handler = new AppClientHandler(interfaces, method, args);
            return executor.submit(handler).get();
        });
    }
}
