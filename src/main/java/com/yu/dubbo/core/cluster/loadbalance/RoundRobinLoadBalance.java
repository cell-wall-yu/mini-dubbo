package com.yu.dubbo.core.cluster.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private static AtomicInteger index = new AtomicInteger(0);

    @Override
    protected String doSelect(List<String> providers, String interfaceName, String methodName) {
        int index = RoundRobinLoadBalance.index.incrementAndGet();
        if (index > providers.size() - 1) {
            index = 0;
            RoundRobinLoadBalance.index.set(index);
        }
        return providers.get(index);
    }
}
