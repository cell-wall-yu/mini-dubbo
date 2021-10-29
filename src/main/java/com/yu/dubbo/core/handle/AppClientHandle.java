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
 * @date 2021/10/29 0029下午 5:20
 */
public class AppClientHandle implements Callable {
    private static Logger log = LoggerFactory.getLogger(AppClientHandle.class);

    private Class<?> cls;
    private String customerAddress;
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

    public static void registAppClientServiceName(String serviceClassName) {

        customerServiceNameList.add(serviceClassName);

        if (allAppServerServiceAddressList.get(serviceClassName) != null) {
            customerServerServiceAddreassList.put(serviceClassName, allAppServerServiceAddressList.get(serviceClassName));
            log.info("[app-client] cache remote app-server interface: {}, address: {}", serviceClassName, JSONObject.toJSONString(allAppServerServiceAddressList.get(serviceClassName)));
        }
    }

    private static AtomicInteger atomicIndexDirect = new AtomicInteger(0);

    public AppClientHandle(Class<?> cls, String customerAddress) {
        this.cls = cls;
        this.customerAddress = customerAddress;
    }

    public AppClientHandle(Class<?> proxy, String localhostHttpAddress, Method method, Object[] args) {
        this.cls = proxy;
        this.customerAddress = localhostHttpAddress;
        this.method = method;
        this.args = args;
    }

    @Override
    public Object call() throws Exception {
        RequestDomain appRequestDomain = null;
        ResponseDomain appResponseDomain = null;
        String appServerAddress = null;
        String status = "success";
        String bizStatus = "success";
        int reqLength = 0;
        int resLength = 0;
        long start = System.currentTimeMillis();
        long end = 0l;
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
                throw new RuntimeException("[rainbow-direct-request] 还没有注册 appServer地址, " + appRequestDomain.getClassName());
            }

            if (appServerAddressList == null || appServerAddressList.isEmpty()) {
                throw new RuntimeException("[rainbow-direct-request] 没有远程 appServer address, " + appRequestDomain.getClassName());
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
            reqLength = data.length;
            // 发送post请求
            byte[] result = HttpPostUtil.request(appServerAddress, null, data);
            resLength = result.length;
            appResponseDomain = CodecUtil.decodeResponse(result);

        } catch (Exception e) {
            // 客户端异常
            appResponseDomain = new ResponseDomain(300, e.getMessage(), e.getMessage());
            log.error("客户端异常:{}", e.getMessage());
            throw new DubboException(e.getMessage());
        } finally {
            end = System.currentTimeMillis();
            int code = appResponseDomain.getCode();
            String errorMsg = null;
            if (code != 0) {
                status = "fail";
                bizStatus = "fail";
                if (code == 200) {
                    errorMsg = "远程 app-server 响应超时";
                } else if (code == 501) {
                    errorMsg = "远程 app-server 端业务异常：" + appResponseDomain.getMessage();
                } else if (code == 300) {
                    errorMsg = "app-client 异常：" + appResponseDomain.getMessage();
                }
            }
//            // 记录通信记录
//            AppClientRequestRecord record = new AppClientRequestRecord();
//            record.setProviderAddress(appServerAddress);
//            record.setCustomerAddress(customerAddress);
//            record.setRequestNo(appRequestDomain.getRequestNo());
//            record.setStatus(status);
//            record.setBizStatus(bizStatus);
//            record.setApiServiceName(appRequestDomain.getClassName());
//            record.setApiServiceMethod(appRequestDomain.getMethodName());
//            record.setBizTimeCost(appResponseDomain.getCostTime());
//            record.setTimeCost((int) (end - start));
//            record.setRequestByteSize(reqLength);
//            record.setResponseByteSize(resLength);
//            record.setErrorMessage(errorMsg);
//            BridgeRecordUtil.add(record);
//            if (code == 501) {
//                throw new BridgeException(errorMsg);
//            }
//        }

            log.info("[app-client] execute done, interface:{}, methodName:{}, cost:{}ms", appRequestDomain.getClassName(), appRequestDomain.getMethodName(), (end - start));
            return appResponseDomain.getResult();
        }
    }
}
