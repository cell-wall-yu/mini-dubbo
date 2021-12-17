package com.yu.dubbo.core.handle;

import com.yu.dubbo.annotation.Reference;
import com.yu.dubbo.core.SpringContextHolder;
import com.yu.dubbo.core.registry.RegistryStrategy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;
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
public class AppClient implements ApplicationContextAware {

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
        // 缓存调用接口类型
        AppClientHandler.cacheServiceName(interfaces.getName());

        AppServiceDomain.Provider provider = RegistryStrategy.getProviderFromRegistry(interfaces.getName());
        // 注册消费者
        RegistryStrategy.registerConsumer(interfaces.getName(), localhostHttpAddress);
        log.info("[app-client] registry customerAddress to registry-center: url: {},service: {}", localhostHttpAddress, interfaces.getName());
        // 缓存type.getName()对应的服务地址
        if (provider != null && provider.getAddressList() != null) {
            AppClientHandler.cacheAppServerAddress(provider.getServiceName(), provider.getAddressList());
        }

        return (T) Proxy.newProxyInstance(AppClient.class.getClassLoader(), new Class<?>[]{interfaces}, (proxy, method, args) -> {
            AppClientHandler handler = new AppClientHandler(interfaces, method, args);
            return executor.submit(handler).get();
        });
    }

    /**
     * 将引用的接口赋上代理对象的值
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
        String[] beanNamesForType = applicationContext.getBeanNamesForType(Object.class);
        for (String beanName : beanNamesForType) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> type = applicationContext.getType(beanName);
            ReflectionUtils.doWithFields(type, field -> {
                if (field.isAnnotationPresent(Reference.class)) {
                    Object proxy = AppClient.proxy(field.getType());
                    field.set(bean, proxy);
                }
            });
        }
    }
}
