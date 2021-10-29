package com.yu.dubbo.core.invoke;

import com.yu.dubbo.core.handle.AppClientHandle;
import com.yu.dubbo.core.registry.RegistryStrategy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.utils.CommonUtil;
import com.yu.dubbo.utils.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:26
 */
public class DubboUtil {
    public static final Logger log = LoggerFactory.getLogger(DubboUtil.class);
    private static ConcurrentHashMap<String, String> customerAddress = new ConcurrentHashMap<>();
    // 线程池
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static int count = 0;

    /**
     * 创建直连代理实例
     *
     * @param interfaces
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T clientProxy(Class<T> interfaces) {
        if (StringUtils.isEmpty(SpringContextHolder.getProperties("spring.application.name")) || StringUtils.isEmpty(SpringContextHolder.getProperties("server.port"))) {
            throw new RuntimeException("请检查应用名{spring.application.name}和端口号{server.port}");
        }

        String localhostHttpAddress = CommonUtil.getLocalServerAddress() +
                "?appName=" + SpringContextHolder.getProperties("spring.application.name") + "&service=" +
                interfaces.getName();

        AppClientHandle.registAppClientServiceName(interfaces.getName());

        if (!customerAddress.containsKey(localhostHttpAddress)) {
            // 注册消费者
            customerAddress.put(localhostHttpAddress, interfaces.getName());
            log.info("[app-server] registry customerAddress to registry-center: url: {},service: {}", localhostHttpAddress, interfaces.getName());
            RegistryStrategy.registerConsumer(interfaces.getName(), localhostHttpAddress);
        }


        // 缓存type.getName()对应的服务地址
        AppServiceDomain.Provider provider = RegistryStrategy.getProviderFromRegistry(interfaces.getName());
        if (provider != null && provider.getAddressList() != null) {
            AppClientHandle.cacheAppServerAddress(provider.getServiceName(), provider.getAddressList());
        }

        return (T) Proxy.newProxyInstance(DubboUtil.class.getClassLoader(), new Class<?>[]{interfaces}, (proxy, method, args) -> {
            AppClientHandle handler = new AppClientHandle(interfaces, localhostHttpAddress, method, args);
            return executor.submit(handler).get();
        });
    }
}

