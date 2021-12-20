package com.yu.dubbo.core.registry;


import com.alibaba.fastjson.JSONObject;
import com.yu.dubbo.annotation.Provider;
import com.yu.dubbo.utils.SpringContextHolder;
import com.yu.dubbo.core.registry.domain.AppDeploy;
import com.yu.dubbo.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AppServiceRegister implements ApplicationContextAware {

    public static String AppserverAddress = "/http_direct/call.action";

    public static final Logger log = LoggerFactory.getLogger(AppServiceRegister.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(Provider.class);
        List<String> beanNames = new ArrayList<>();
        for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
            beanNames.add(entry.getKey());
        }
        if (StringUtils.isEmpty(SpringContextHolder.getProperties("spring.application.name")) || StringUtils.isEmpty(SpringContextHolder.getProperties("server.port"))) {
            throw new RuntimeException("please check server name{spring.application.name} and port{server.port}");
        }

        String providerAddress = CommonUtil.getLocalServerAddress() + "/" + SpringContextHolder.getProperties("spring.application.name") + AppserverAddress +
                "?weight=" + (StringUtils.isEmpty(SpringContextHolder.getProperties("spring.mini-dubbo.weight")) ? 1 : SpringContextHolder.getProperties("spring.mini-dubbo.weight"));

        for (String beanName : beanNames) {
            if (beanName.indexOf("org.springframework") != -1) {
                continue;
            }
            Class<?> type = applicationContext.getType(beanName);
            String typeName = type.getName();
            // spring 代理类
            if (typeName.indexOf("$") != -1) {
                typeName = typeName.substring(0, typeName.indexOf("$"));
                try {
                    type = Class.forName(typeName);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            if (type.isAnnotationPresent(Provider.class)) {
                Class<?>[] define = type.getInterfaces();
                for (Class<?> cls : define) {

                    if (cls.getName().indexOf("org.springframework") != -1) {
                        continue;
                    }

                    if (cls.getName().startsWith("private")) {
                        throw new RuntimeException("接口名不能以private开头 ");
                    }
                    log.info("[app-server] registry providerAddress to registry-center: {}, url: {}", cls.getName(), providerAddress);
                    RegistryStrategy.registerProvider(cls.getName(), providerAddress);
                }
            }
        }
        // 注册部署应用信息
        registerAppDeploy();
    }


    public void registerAppDeploy() {
        try {
            String appName = SpringContextHolder.getProperties("spring.application.name");
            String hostName = InetAddress.getLocalHost().getHostName();
            String deployUri = CommonUtil.getLocalServerAddress();
            String pid = System.getProperty("PID");
            String userHome = System.getProperty("user.name");
            String appStartTime = CommonUtil.format(new Date(), CommonUtil.yyyyMMhhmmss);
            String userDir = CommonUtil.getAppHomePath();
            String osName = System.getProperty("os.name");
            AppDeploy ap = new AppDeploy();
            ap.setAppName(appName);
            ap.setHostName(hostName);
            ap.setPid(pid);
            ap.setUserHome(userHome);
            ap.setDeployUri(deployUri);
            ap.setAppStartTime(appStartTime);
            ap.setAppDeployPath(userDir);
            ap.setOsName(osName);
            RegistryStrategy.registerAppDeploy(appName, ap);
            log.info("[app-server] registry app-deploy to registry-center: {}", JSONObject.toJSONString(ap));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
