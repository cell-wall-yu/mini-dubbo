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
    public static ConcurrentHashMap<String, List<String>> serverServiceAddressList = new ConcurrentHashMap<String, List<String>>();

    /**
     * zk中所有地址缓存
     */
    public static ConcurrentHashMap<String, List<String>> allServerServiceAddressList = new ConcurrentHashMap<String, List<String>>();

    /**
     * 远程接口名缓存
     */
    public static List<String> serviceNameList = new ArrayList<String>();

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
            allServerServiceAddressList.remove(serviceClassName);
            if (serverServiceAddressList.get(serviceClassName) != null) {
                serverServiceAddressList.remove(serviceClassName);
                log.info("[app-client] remote app-server interface is offline, interface: {}", serviceClassName);
            }
            return;
        }

        allServerServiceAddressList.put(serviceClassName, appServerAddressList);
        if (serviceNameList.contains(serviceClassName)) {
            if (serverServiceAddressList.get(serviceClassName) != null) {
                if (CommonUtil.equals(serverServiceAddressList.get(serviceClassName), appServerAddressList)) {
                    return;
                }
            }
            serverServiceAddressList.put(serviceClassName, appServerAddressList);
            log.info("[app-client] cache remote app-server interface: {}, address: {}", serviceClassName, JSONObject.toJSONString(appServerAddressList));
        }
    }

    /**
     * 缓存接口名
     *
     * @param serviceClassName
     */
    public static void cacheServiceName(String serviceClassName) {
        serviceNameList.add(serviceClassName);
        if (allServerServiceAddressList.get(serviceClassName) != null) {
            serverServiceAddressList.put(serviceClassName, allServerServiceAddressList.get(serviceClassName));
            log.info("[app-client] cache remote app-server interface: {}, address: {}", serviceClassName, JSONObject.toJSONString(allServerServiceAddressList.get(serviceClassName)));
        }
    }

    /**
     * 移除下线服务提供的接口地址
     *
     * @param serviceClassName
     * @param address
     */
    public static void removeAppServerAddress(String serviceClassName, String address) {
        if (allServerServiceAddressList.get(serviceClassName) != null) {
            List<String> addressList = serverServiceAddressList.get(serviceClassName);
            if (!addressList.isEmpty() && addressList.size() == 1) {
                serverServiceAddressList.remove(serviceClassName);
                allServerServiceAddressList.remove(serviceClassName);
            }
            if (addressList.size() > 1) {
                addressList.remove(address);
            }
        }
        if (log.isInfoEnabled()) {
            log.info("[app-client] remote app-server  interface is offline: {}, address: {}", serviceClassName, address);
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

            List<String> appServerAddressList = serverServiceAddressList.get(appRequestDomain.getClassName());

            if (appServerAddressList == null || appServerAddressList.isEmpty()) {
                throw new DubboException("[direct-request] not register appServerAddress, " + appRequestDomain.getClassName());
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
            log.error("[app-client] client exception :{}", e.getMessage());
        } finally {
            long end = System.currentTimeMillis();
            log.info("[app-client] execute done, target address:{} interface:{}, methodName:{}, cost:{}ms", appServerAddress, appRequestDomain.getClassName(), appRequestDomain.getMethodName(), (end - start));
            if (0 != appResponseDomain.getCode()) {
                throw new DubboException(appResponseDomain.getMessage());
            } else {
                return appResponseDomain.getResult();
            }
        }
    }
}
