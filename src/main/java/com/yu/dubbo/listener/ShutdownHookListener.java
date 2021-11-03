package com.yu.dubbo.listener;

import com.yu.dubbo.core.registry.RegistryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * @author Administrator
 * @title: ShutdownHookListener
 * @projectName bridge
 * @description: TODO
 * @date 2021/10/20 0020上午 10:51
 */

public class ShutdownHookListener implements ApplicationListener {

    private final static Logger log = LoggerFactory.getLogger(ShutdownHookListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextClosedEvent) {
            // kill -15 ,程序上的优雅关闭，而系统上的关闭无法执行如 kill -9
            RegistryStrategy.destroy();
            log.info("[ShutdownHookListener] listener {}", applicationEvent.getClass().getName());

        }
    }
}
