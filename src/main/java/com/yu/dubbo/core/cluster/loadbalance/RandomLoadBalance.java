package com.yu.dubbo.core.cluster.loadbalance;

import com.yu.dubbo.core.protocol.RequestDomain;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "random";

    @Override
    protected String doSelect(List<String> providers, RequestDomain requestDomain) {       int length = providers.size();
        int totalWeight = 0;
        boolean sameWeight = true;
        for (int i = 0; i < length; i++) {
            int weight = getWeight(providers.get(i));
            totalWeight += weight;
            if (sameWeight && i > 0
                    && weight != getWeight(providers.get(i - 1))) {
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // Return a invoker based on the random value.
            // 可以理解成：[0,totalWeight)取随机数，看这个随机数(每比较一次，减去响应的权重)
            // 落在了以权重为刻度的数轴哪个区间内，落在那个区间即返回哪个provider
            for (int i = 0; i < length; i++) {
                offset -= getWeight(providers.get(i));
                if (offset < 0) {
                    return providers.get(i);
                }
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        return providers.get(ThreadLocalRandom.current().nextInt(length));
    }
}
