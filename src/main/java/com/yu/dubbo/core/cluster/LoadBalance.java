package com.yu.dubbo.core.cluster;

import java.util.List;

public interface LoadBalance {
    /**
     * 通过负载均衡算法选取集群中的服务提供者的地址
     *
     * @param providers
     * @return
     * @throws Exception
     */
    String select(List<String> providers, String interfaceName, String methodName) throws Exception;
}
