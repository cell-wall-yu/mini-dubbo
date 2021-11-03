package com.yu.dubbo.core.handle;

import com.alibaba.fastjson.JSONObject;
import com.yu.dubbo.core.codec.CodecUtil;
import com.yu.dubbo.core.protocol.RequestDomain;
import com.yu.dubbo.core.protocol.ResponseDomain;
import com.yu.dubbo.exception.DubboException;
import com.yu.dubbo.utils.CommonUtil;
import com.yu.dubbo.utils.HttpPostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/11/1 0001下午 3:40
 */
public class AppClientHandler implements Callable {
    private static Logger log = LoggerFactory.getLogger(AppClientHandler.class);

    private Class<?> cls;
    private Method method;
    private Object[] args;
    /**
     * 消费端远程地址缓存
     */
    private static ConcurrentHashMap<String, List<String>> customerServerServiceAddreassList = new ConcurrentHashMap<String, List<String>>();

    /**
     * zk中所有地址缓存
     */
    public static ConcurrentHashMap<String, List<String>> allAppServerServiceAddressList = new ConcurrentHashMap<String, List<String>>();

    /**
     * 远程接口名缓存
     */
    public static List<String> customerServiceNameList = new ArrayList<String>();


    /**
     * 获取直连模式所有服务生产者的地址
     *
     * @param interfaceName 接口名
     * @return
     * @author Wang Xiaobo 2020年2月13日
     */
    public static List<String> getDirectProviderServerAddress(String interfaceName) {
        return allAppServerServiceAddressList.get(interfaceName);
    }

    /**
     * 监听并缓存appServer端接口地址，只缓存声明过 http 直连类型的接口名
     *
     * @param serviceClassName
     * @param appServerAddressList
     */
    public static void cacheAppServerAddress(String serviceClassName, List<String> appServerAddressList) {
        if (appServerAddressList == null || appServerAddressList.isEmpty()) {
            log.info("[app-client] remote app-server interface seems to be offline, interface: {}", serviceClassName);
            return;
        }

        if (appServerAddressList == null || appServerAddressList.isEmpty()) {
            allAppServerServiceAddressList.remove(serviceClassName);
            if (customerServerServiceAddreassList.get(serviceClassName) != null) {
                customerServerServiceAddreassList.remove(serviceClassName);
                log.info("[app-client] remote app-server interface is offline, interface: {}", serviceClassName);
            }
            return;
        }

        allAppServerServiceAddressList.put(serviceClassName, appServerAddressList);
        if (customerServiceNameList.contains(serviceClassName)) {
            if (customerServerServiceAddreassList.get(serviceClassName) != null) {
                if (CommonUtil.equals(customerServerServiceAddreassList.get(serviceClassName), appServerAddressList)) {
                    return;
                }
            }
            customerServerServiceAddreassList.put(serviceClassName, appServerAddressList);
            log.info("[app-client] cache remote app-server interface: {}, address: {}", serviceClassName, JSONObject.toJSONString(appServerAddressList));
        }
    }

    public static void registerAppClientServiceName(String serviceClassName) {
        customerServiceNameList.add(serviceClassName);
        if (allAppServerServiceAddressList.get(serviceClassName) != null) {
            customerServerServiceAddreassList.put(serviceClassName, allAppServerServiceAddressList.get(serviceClassName));
            log.info("[app-client] cache remote app-server interface: {}, address: {}", serviceClassName, JSONObject.toJSONString(allAppServerServiceAddressList.get(serviceClassName)));
        }
    }

    private static AtomicInteger atomicIndexDirect = new AtomicInteger(0);

    public AppClientHandler(Class<?> proxy, Method method, Object[] args) {
        this.cls = proxy;
        this.method = method;
        this.args = args;
    }

    @Override
    public Object call() {
        log.info("current thread name: {}", Thread.currentThread().getName());
        RequestDomain appRequestDomain = null;
        ResponseDomain appResponseDomain = null;
        String appServerAddress = null;
        long start = System.currentTimeMillis();
        try {
            String className = cls.getName();
            String methodName = method.getName();

            String[] paramTypeNames = null;
            Parameter[] methodParameters = method.getParameters();
            if (methodParameters != null && methodParameters.length > 0) {
                paramTypeNames = new String[methodParameters.length];

                for (int i = 0; i < methodParameters.length; i++) {
                    paramTypeNames[i] = methodParameters[i].getType().getName();
                }
            }
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(cls, args);
            }
            if ("toString".equals(methodName) && paramTypeNames.length == 0) {
                return cls.toString();
            }
            if ("hashCode".equals(methodName) && paramTypeNames.length == 0) {
                return cls.hashCode();
            }
            if ("equals".equals(methodName) && paramTypeNames.length == 1) {
                return cls.equals(args[0]);
            }
            // 请求参数
            String requestNo = String.valueOf(UUID.randomUUID());
            appRequestDomain = new RequestDomain();
            appRequestDomain.setRequestNo(requestNo);
            appRequestDomain.setClassName(className);
            appRequestDomain.setMethodName(methodName);
            appRequestDomain.setParamTypeNames(paramTypeNames);
            appRequestDomain.setParamInputs(args);

            List<String> appServerAddressList = customerServerServiceAddreassList.get(appRequestDomain.getClassName());

            if (appServerAddressList == null || appServerAddressList.isEmpty()) {
                throw new DubboException("[direct-request] 还没有注册 appServer地址, " + appRequestDomain.getClassName());
            }

            // 服务提供者集群中轮询调用
            if (appServerAddressList.size() == 1) {
                appServerAddress = appServerAddressList.get(0);
            } else {
                int index = atomicIndexDirect.incrementAndGet();
                if (index > appServerAddressList.size() - 1) {
                    index = 0;
                    atomicIndexDirect.set(index);
                }
                appServerAddress = appServerAddressList.get(index);
            }

            // 请求参数编码
            byte[] data = CodecUtil.encodeRequest(appRequestDomain);
            // 发送post请求
            byte[] result = HttpPostUtil.request(appServerAddress, null, data);
            appResponseDomain = CodecUtil.decodeResponse(result);

        } catch (Exception e) {
            // 客户端异常
            appResponseDomain = new ResponseDomain(300, e.getMessage(), e.getMessage());
            log.error("客户端异常:{}", e.getMessage());
            throw new DubboException(e.getMessage());
        } finally {
            long end = System.currentTimeMillis();
            log.info("[app-client] execute done, interface:{}, methodName:{}, cost:{}ms", appRequestDomain.getClassName(), appRequestDomain.getMethodName(), (end - start));
            return appResponseDomain.getResult();
        }
    }
}
