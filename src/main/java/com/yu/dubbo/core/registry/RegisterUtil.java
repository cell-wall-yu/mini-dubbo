package com.yu.dubbo.core.registry;

import com.yu.dubbo.core.handle.AppServer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.util.StringUtils;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:14
 */
public class RegisterUtil {
    /**
     * 注册自定义servlet监听/http_direct/call.action
     *
     * @return
     */
    public static ServletRegistrationBean customServlet(String appserverAddress) {
        if (!StringUtils.isEmpty(appserverAddress)) {
            if (appserverAddress.startsWith("/")) {
                AppServiceRegister.AppserverAddress = appserverAddress;
            } else {
                AppServiceRegister.AppserverAddress = "/" + appserverAddress;
            }
        }
        return new ServletRegistrationBean(new AppServer(), AppServiceRegister.AppserverAddress);
    }
}
