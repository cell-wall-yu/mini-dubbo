package com.yu.dubbo.core.config;

import com.yu.dubbo.core.handle.AppServer;
import com.yu.dubbo.core.registry.AppServiceRegister;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({ServletWebServerFactoryAutoConfiguration.class})
public class MiniDubboServerAutoConfiguration {
    private static final String DEFAULT_MINI_DUBBO_SERVLET_BEAN_NAME = "miniDubboServerServlet";
    public static final String DEFAULT_MINI_DUBBO_SERVLET_BEAN_NAME_REGISTRATION_BEAN_NAME = "miniDubboServerServletRegistration";

    @Bean(name = {DEFAULT_MINI_DUBBO_SERVLET_BEAN_NAME_REGISTRATION_BEAN_NAME})
    public ServletRegistrationBean miniMvcServletFastRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new AppServer(), AppServiceRegister.AppserverAddress);
        registration.setName(DEFAULT_MINI_DUBBO_SERVLET_BEAN_NAME);
        return registration;
    }
}
